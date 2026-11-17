import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val AGENT_SHA256 = "411587286dbb83b06fcdd042ad1c9c1818655831366717aef3611962ea2c1b73"

class NewRelicAgentPluginTest {
    private val project: File = Files.createTempDirectory("newrelic-agent-plugin").toFile()

    @AfterTest
    fun cleanUp() {
        project.deleteRecursively()
    }

    private fun build(
        name: String,
        vararg arguments: String,
    ) = run {
        project.resolve("settings.gradle.kts").writeText("rootProject.name = \"$name\"\n")
        project.resolve("build.gradle.kts").writeText(
            """
            plugins { id("jjt.newrelic-agent") }
            repositories { mavenCentral() }
            tasks.register("bootJar")
            """.trimIndent(),
        )
        GradleRunner
            .create()
            .withProjectDir(project)
            .withPluginClasspath()
            .withArguments(*arguments)
    }

    @Test
    fun `boot jar prepares the pinned agent, configuration and executable script`() {
        val result = build("demo-service", "bootJar", "--configuration-cache").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":prepareNewRelicAgent")?.outcome)
        val output = project.resolve("build/newrelic")
        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(output.resolve("newrelic.jar").readBytes())
                .joinToString("") { "%02x".format(it) }
        assertEquals(AGENT_SHA256, digest)
        assertEquals(conventionResource("/jjt/newrelic/newrelic.yml"), output.resolve("newrelic.yml").readText())
        val script = output.resolve("start-service.sh")
        assertTrue(script.canExecute())
        assertTrue(script.readText().contains("SERVICE_NAME='demo-service'"))
    }

    @Test
    fun `an unexpected project name is rejected`() {
        val result = build("Demo_Service", "bootJar").buildAndFail()

        assertTrue(result.output.contains("Unexpected service name for New Relic"))
    }
}
