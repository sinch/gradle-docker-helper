import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import java.io.File

class IntegrationTests : StringSpec() {
    companion object {
        const val PROJECT_DIR_ROOT = "src/test/resources"

        val GRADLE_TEST_TASKS = listOf("tasks", "--all")
    }

    fun getGradleRunnerOutput(projectDir: String): String {
        val output =
            GradleRunner
                .create()
                .withProjectDir(File("$PROJECT_DIR_ROOT/$projectDir"))
                .withPluginClasspath()
                .withArguments(GRADLE_TEST_TASKS)
                .build()
                .output
        println(output)
        return output
    }

    init {
        "default task prefix" {
            val output = getGradleRunnerOutput("default-task-prefix")
            listOf(
                "Docker tasks",
                "dockerCreateContainer",
                "dockerPullImage",
                "dockerRemoveContainer",
                "dockerStartAndWaitForContainer",
                "dockerStartContainer",
                "dockerStopAndRemoveContainer",
                "dockerStopContainer",
            ).forEach { output shouldContain it }
        }

        "custom task prefix" {
            val output = getGradleRunnerOutput("custom-task-prefix")
            listOf(
                "Foobar tasks",
                "fooCreateContainer",
                "fooPullImage",
                "fooRemoveContainer",
                "fooStartAndWaitForContainer",
                "fooStartContainer",
                "fooStopAndRemoveContainer",
                "fooStopContainer",
            ).forEach { output shouldContain it }
        }
    }
}
