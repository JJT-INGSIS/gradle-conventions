import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NewRelicConfigurationTest {
    private val configuration = conventionResource("/jjt/newrelic/newrelic.yml")

    private fun assertSetting(expected: String) = assertTrue(configuration.contains(expected), expected)

    @Test
    fun `the license is never part of the configuration`() {
        assertSetting("license_key: ''")
    }

    @Test
    fun `agent logs go to the container output`() {
        assertSetting("log_file_name: STDOUT")
    }

    @Test
    fun `traces use only w3c headers and follow the remote parent decision`() {
        assertSetting("exclude_newrelic_header: true")
        assertSetting("remote_parent_sampled: always_on")
        assertSetting("remote_parent_not_sampled: always_off")
    }

    @Test
    fun `requests, sql and exceptions do not leak user data`() {
        listOf("request.headers.*", "request.parameters.*", "response.headers.*", "message.parameters.*")
            .forEach { assertSetting("- $it") }
        assertSetting("record_sql: obfuscated")
        assertSetting("explain_enabled: false")
        assertSetting("strip_exception_messages:\n    enabled: true")
        assertFalse(configuration.contains("high_security: true"))
    }

    @Test
    fun `client errors are expected and logs carry only the allowed context`() {
        assertSetting("expected_status_codes: 400-499")
        listOf("request.id", "operation", "outcome", "duration.ms", "correlation.id", "event.id", "job.id", "attempt")
            .forEach { assertSetting("- $it") }
        assertSetting("local_decorating:\n      enabled: false")
    }
}
