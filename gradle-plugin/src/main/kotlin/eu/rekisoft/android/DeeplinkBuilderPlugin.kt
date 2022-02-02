package eu.rekisoft.android

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.gradle.internal.operations.DefaultBuildOperationIdFactory
import org.gradle.internal.time.Time
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withConvention
import org.gradle.tooling.internal.consumer.SynchronizedLogging
import org.slf4j.LoggerFactory
import java.io.File

open class DeeplinkBuilderPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = with(project) {


        val generate = tasks.register("generateDeeplinks", GenerateTask::class.java) {
            val androidSourceSets = project.the<ApplicationExtension>().sourceSets
            val inputDirs = mutableListOf<File>()
            androidSourceSets.forEach { sourceSet ->
                sourceSet.resources.srcDirs("build/deeplink-builder/res/${sourceSet.name}/")
                inputDirs += File(projectDir, "src/${sourceSet.name}/res/navigation")
            }

            inputFiles += inputDirs.listFilesByExtension("xml")
            outputDirectories += androidSourceSets.map {
                File(
                    buildDir,
                    "deeplink-builder/res/${it.name}"
                )
            }
        }

        //extensions.withConvention(ApplicationExtension::class) {
        //    tasks.getByName("processResources").dependsOn += generate
        //}
    }
}

private fun Iterable<File>.listFilesByExtension(vararg extensions: String) =
    flatMap { dir ->
        if (dir.exists()) {
            dir.listFiles { _, name ->
                extensions.any { extension ->
                    name.endsWith(".$extension")
                }
            }.toList()
        } else {
            emptyList()
        }
    }

@CacheableTask
open class GenerateTask : DefaultTask() {

    private val progressLoggerFactory = SynchronizedLogging(Time.clock(), DefaultBuildOperationIdFactory()).progressLoggerFactory
    private val LOGGER = LoggerFactory.getLogger(GenerateTask::class.java)

    @InputFiles
    @PathSensitive(value = PathSensitivity.NONE)
    val inputFiles = mutableListOf<File>()

    @OutputDirectories
    val outputDirectories = mutableListOf<File>()

    @TaskAction
    fun generate() {
        val op = progressLoggerFactory.newOperation(GenerateTask::class.java)
        //op.loggingHeader = "header"
        op.description = "description"
        //org.gradle.api.logging.Logger().lifecycle("blah")
        //op.setShortDescription("description")
        //val foo = op.start("description", "description")
        LOGGER.info("Processing ${inputFiles.size} files... ${inputFiles.joinToString()}")
        op.started()
        inputFiles.forEach { file ->
            Thread.sleep(5000)
            op.progress("${op.description}: ${file.path}")
            LOGGER.debug("${op.description}: ${file.path}")
            val start = file.path.indexOf("src" + File.separator) + 4
            val end = file.path.indexOf(File.separatorChar, start)
            val sourceSet = file.path.substring(start, end)
        }
        op.completed()
    }
}