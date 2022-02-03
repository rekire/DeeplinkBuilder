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
            val fragments = doc.documentElement.childNodes.toList().findAll("fragment")
            logger.info("Found ${fragments.size} fragments in graph")
            fragments.forEach { fragment ->
                val fqn = fragment.attributes.getNamedItem("android:name")?.nodeValue
                if (fqn == null) {
                    logger.warn("Skipping fragment with missing attribute android:name")
                } else {
                    fragment.childNodes.toList().findAll("deepLink").forEach { link ->
                        val uri = link.attributes.getNamedItem("app:uri")?.nodeValue
                        if (uri == null) {
                            logger.error("Deeplink of Fragment \"${fqn.substringAfterLast('.')}\" has no app:uri attribute")
                        }
                        //println(" -> " + link.attributes.getNamedItem("app:uri")?.nodeValue)
                    }
                }
            }
            //println("Found ${deeplinks.size} deeplinks")
        }
    }

    private fun NodeList.toList(): List<Node> {
        val result = mutableListOf<Node>()
        if (length == 0) return result
        for (i in 0 until length) {
            result.add(item(i))
        }
        return result
    }

    private fun List<Node>.findAll(tag: String): List<Element> =
        flatMap { node ->
            if (node is Element) {
                if (node.tagName == tag) listOf(node)
                else node.childNodes.toList().findAll(tag)
            } else emptyList()
        }
}