package app.awero.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncApiConfigurationTest {
    @Test
    fun missingOrNonHttpsEndpointLeavesSyncDisabled() {
        assertFalse(SyncApiConfiguration.fromValue(null).isEnabled)
        assertFalse(SyncApiConfiguration.fromValue(" ").isEnabled)
        assertNull(SyncApiConfiguration.fromValue("http://api.example.test/api/v1").baseUrl)
        assertNull(SyncApiConfiguration.fromValue("https://user:pass@api.example.test/api/v1").baseUrl)
    }

    @Test
    fun httpsEndpointIsNormalizedAndBuildsSafePaths() {
        val config = SyncApiConfiguration.fromValue(" https://api.example.test/api/v1/ ")

        assertTrue(config.isEnabled)
        assertEquals("https://api.example.test/api/v1/health", config.endpoint("/health").toString())
        assertNull(config.endpoint("../private"))
        assertNull(config.endpoint("%2e%2e/private"))
        assertNull(config.endpoint("health?override=true"))
    }
}
