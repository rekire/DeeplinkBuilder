package eu.rekisoft.android

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

open class DeeplinkBuilderPlugin @Inject constructor(val providerFactory: ProviderFactory) : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("com.android.application") {
            project.extensions.getByType(BaseExtension::class.java).forEachVariant { variant ->
                val task = project.tasks.register(
                    "generate${variant.name.capitalize()}Deeplinks",
                    GenerateTask::class.java
                ) {
                    it.inputFiles.setFrom(project.navigationFiles(variant))
                    it.outputDir.set(
                        File(
                            project.buildDir,
                            "generated/source/deeplink-builder/${variant.dirName}"
                        )
                    )
                }
                project.tasks.getByName("generate${variant.name.capitalize()}Sources").dependsOn += task
            }
        }
    }

    private fun BaseExtension.forEachVariant(action: (BaseVariant) -> Unit) {
        when (this) {
            is AppExtension -> applicationVariants.all(action)
            is LibraryExtension -> {
                libraryVariants.all(action)
            }
            else -> throw GradleException(
                "safeargs plugin must be used with android app," +
                        "library or feature plugin"
            )
        }
    }

    @Suppress("DEPRECATION") // For BaseVariant should be replaced in later studio versions
    private fun Project.navigationFiles(variant: BaseVariant): ConfigurableFileCollection {
        val fileProvider = providerFactory.provider {
            variant.sourceSets
                .flatMap { it.resDirectories }
                .mapNotNull {
                    File(it, "navigation").let { navFolder ->
                        if (navFolder.exists() && navFolder.isDirectory) navFolder else null
                    }
                }
                .flatMap { navFolder -> navFolder.listFiles()?.asIterable() ?: emptyList() }
                .filter { file -> file.isFile }
                .groupBy { file -> file.name }
                .map { entry -> entry.value.last() }
        }
        return files(fileProvider)
    }
}

@CacheableTask
abstract class GenerateTask : DefaultTask() {
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:InputFiles
    abstract val inputFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val outputDirFile = outputDir.asFile.get()
        if (outputDirFile.exists() && !outputDirFile.deleteRecursively()) {
            logger.warn("Failed to clear directory for deeplink builder")
        }
        logger.info("Processing ${inputFiles.from.size} files...")
        inputFiles.forEach { file ->
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(file.inputStream())
            require(doc.documentElement.tagName == "navigation") { "${file.name} has unexpected content" }
            val fragments = doc.documentElement.childNodes.findAll("fragment")
            logger.info("Found ${fragments.size} fragments in graph")
            fragments.forEach { fragment ->
                val fqn = fragment.attributes.getNamedItem("android:name")?.nodeValue
                if (fqn == null) {
                    logger.warn("Skipping fragment with missing attribute android:name")
                } else {
                    fragment.childNodes.findAll("deepLink").forEach { link ->
                        generateCode(fqn, fragment, link)?.let { code ->
                            val codeFile = File(
                                outputDirFile,
                                fqn.replace(".", File.separator) + "Deeplink.kt"
                            )
                            codeFile.parentFile.mkdirs()
                            codeFile.outputStream().use { fos ->
                                fos.write(code.toByteArray())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun generateCode(fqn: String, fragment: Element, link: Element): String? {
        val uri = link.attributes.getNamedItem("app:uri")?.nodeValue
        return if (uri == null) {
            logger.error("Deeplink of Fragment \"${fqn.substringAfterLast('.')}\" has no app:uri attribute")
            null
        } else {
            val sb = StringBuilder()
            sb.appendLine("package ${fqn.substringBeforeLast('.')}").appendLine()
            sb.appendLine("import android.net.Uri")
            sb.appendLine("import androidx.navigation.NavDeepLinkRequest").appendLine()
            sb.appendLine("object ${fqn.substringAfterLast('.')}Deeplink {")
            sb.appendLine("    @JvmStatic")
            val args = parseArguments(fragment)
            sb.append("    fun create(")
            args.forEachIndexed { i, arg ->
                if (i > 0) print(", ")
                sb.append("${arg.name}: ${arg.type}")
                if (arg.nullable) {
                    sb.append("?")
                }
                arg.defaultValue?.let { value ->
                    if (value == "@null" && arg.nullable) {
                        sb.append(" = null")
                    } else {
                        sb.append(" = ${arg.defaultValue}")
                    }
                }
            }
            sb.appendLine(") = NavDeepLinkRequest.Builder.fromUri(")
            sb.appendLine("        Uri.parse(\"${uri.replace("{", "\${")}\")")
            sb.appendLine("    ).build()")
            sb.append("}")
            sb.toString()
        }
    }

    private fun parseArguments(fragment: Element) =
        fragment.findAll("argument", false).map { arg ->
            val name =
                requireNotNull(arg.attributes.getNamedItem("android:name")?.nodeValue) { "Argument has no android:name" }
            val rawType = arg.attributes.getNamedItem("app:argType")?.nodeValue
            val type = when (rawType) {
                null -> throw IllegalArgumentException("Argument has no app:argType")
                "integer" -> "Int"
                "string" -> "String"
                "float" -> "Float"
                "long" -> "Long"
                "boolean" -> "Boolean"
                "reference" -> "Int"
                // TODO array types
                else -> {
                    val clazz = Class.forName(rawType)
                    when {
                        clazz.isEnum -> rawType
                        else -> throw IllegalArgumentException("Type \"$rawType\" is not supported")
                    }
                }
            }
            var defaultValue =
                arg.attributes.getNamedItem("android:defaultValue")?.nodeValue
            val nullable =
                arg.attributes.getNamedItem("app:nullable")?.nodeValue == "true"
            if (defaultValue != null && rawType == "string") {
                defaultValue = "\"$defaultValue\""
            }
            Argument(name, type, nullable, defaultValue)
        }

    private data class Argument(
        val name: String,
        val type: String,
        val nullable: Boolean,
        val defaultValue: String?
    )

    private fun NodeList.toList(): List<Node> {
        val result = mutableListOf<Node>()
        if (length == 0) return result
        for (i in 0 until length) {
            result.add(item(i))
        }
        return result
    }

    private fun NodeList.findAll(tag: String, searchRecursive: Boolean = true): List<Element> =
        toList().findAll(tag, searchRecursive)

    private fun Element.findAll(tag: String, searchRecursive: Boolean = true): List<Element> =
        childNodes.findAll(tag, searchRecursive)

    private fun List<Node>.findAll(tag: String, searchRecursive: Boolean = true): List<Element> =
        flatMap { node ->
            if (node is Element) {
                when {
                    node.tagName == tag -> listOf(node)
                    searchRecursive -> node.childNodes.toList().findAll(tag)
                    else -> emptyList()
                }
            } else emptyList()
        }
}