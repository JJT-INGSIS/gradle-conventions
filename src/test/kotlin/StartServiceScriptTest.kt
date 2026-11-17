import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SERVICE = "demo-service"
private const val APPLICATION_JAR = "/app/app.jar"
private const val VALID_LICENSE = "fixture-license-0123456789abcdef"
private const val CONFIGURATION_ERROR = 64
private const val SUCCESS = 0

class StartServiceScriptTest {
    private val workspace: File = Files.createTempDirectory("start-service").toFile()
    private val agentDirectory = workspace.resolve("newrelic").apply { mkdirs() }
    private val binaries = workspace.resolve("bin").apply { mkdirs() }
    private val invocation = workspace.resolve("invocation.txt")
    private val licenseFile = workspace.resolve("license").apply { writeText(VALID_LICENSE + "\n") }

    init {
        agentDirectory.resolve("start-service.sh").apply {
            writeText(conventionResource("/jjt/newrelic/start-service.sh").replace("@SERVICE_NAME@", SERVICE))
            setExecutable(true)
        }
        binaries.resolve("java").apply {
            writeText(
                """
                #!/bin/sh
                {
                  printf 'arg=%s\n' "${'$'}@"
                  printf 'app=%s\n' "${'$'}{NEW_RELIC_APP_NAME:-}"
                  printf 'labels=%s\n' "${'$'}{NEW_RELIC_LABELS:-}"
                  printf 'key=%s\n' "${'$'}{NEW_RELIC_LICENSE_KEY:-}"
                } > "${invocation.absolutePath}"
                """.trimIndent(),
            )
            setExecutable(true)
        }
    }

    @AfterTest
    fun cleanUp() {
        workspace.deleteRecursively()
    }

    private data class Result(
        val exitCode: Int,
        val errors: String,
    )

    private fun start(environment: Map<String, String>): Result {
        val process =
            ProcessBuilder("sh", agentDirectory.resolve("start-service.sh").absolutePath, APPLICATION_JAR).apply {
                environment().clear()
                environment()["PATH"] = binaries.absolutePath + File.pathSeparator + "/usr/bin:/bin"
                environment().putAll(environment)
            }.start()
        val errors = process.errorStream.bufferedReader().readText()
        return Result(process.waitFor(), errors)
    }

    private fun enabled(extra: Map<String, String> = emptyMap()): Map<String, String> =
        mapOf(
            "NEW_RELIC_ENABLED" to "true",
            "NEW_RELIC_ENVIRONMENT" to "dev",
            "NEW_RELIC_LICENSE_FILE" to licenseFile.absolutePath,
            "NEW_RELIC_SERVICE_VERSION" to "1db4634",
        ) + extra

    private fun recorded(): List<String> = invocation.readLines()

    @Test
    fun `disabled by default starts the application exactly as before`() {
        val result = start(emptyMap())

        assertEquals(SUCCESS, result.exitCode)
        assertEquals(listOf("arg=-jar", "arg=$APPLICATION_JAR", "app=", "labels=", "key="), recorded())
    }

    @Test
    fun `enabled attaches the bundled agent with its configuration`() {
        val result = start(enabled())

        assertEquals(SUCCESS, result.exitCode)
        val agentPath = agentDirectory.absolutePath
        assertEquals(
            listOf(
                "arg=-javaagent:$agentPath/newrelic.jar",
                "arg=-Dnewrelic.config.file=$agentPath/newrelic.yml",
                "arg=-jar",
                "arg=$APPLICATION_JAR",
                "app=$SERVICE (dev)",
                "labels=environment:dev;service:$SERVICE;version:1db4634",
                "key=$VALID_LICENSE",
            ),
            recorded(),
        )
    }

    @Test
    fun `unknown flags and environments fail before starting java`() {
        val attempts =
            listOf(
                enabled(mapOf("NEW_RELIC_ENABLED" to "yes")),
                enabled(mapOf("NEW_RELIC_ENVIRONMENT" to "staging")),
                enabled(mapOf("NEW_RELIC_ENVIRONMENT" to "")),
                enabled(mapOf("NEW_RELIC_SERVICE_VERSION" to "sha256:abc;service:other")),
            )

        attempts.forEach { attempt ->
            assertEquals(CONFIGURATION_ERROR, start(attempt).exitCode, attempt.toString())
        }
        assertFalse(invocation.exists())
    }

    @Test
    fun `a missing or malformed license fails without echoing it`() {
        val malformed = workspace.resolve("malformed").apply { writeText("$VALID_LICENSE extra\n") }
        val missing = start(enabled(mapOf("NEW_RELIC_LICENSE_FILE" to workspace.resolve("absent").absolutePath)))
        val invalid = start(enabled(mapOf("NEW_RELIC_LICENSE_FILE" to malformed.absolutePath)))

        assertEquals(CONFIGURATION_ERROR, missing.exitCode)
        assertEquals(CONFIGURATION_ERROR, invalid.exitCode)
        assertFalse(invalid.errors.contains(VALID_LICENSE))
        assertFalse(invocation.exists())
    }

    @Test
    fun `the license never reaches the java command line`() {
        start(enabled())

        assertTrue(recorded().filter { it.startsWith("arg=") }.none { it.contains(VALID_LICENSE) })
    }
}
