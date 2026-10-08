package app.awero.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WakePersistenceUnitTest {
    @Test
    fun migrationEndsAtVersionTwo() {
        assertEquals(2, AweroDatabase.MIGRATION_1_2.endVersion)
        assertTrue(AweroDatabase.MIGRATION_1_2.startVersion == 1)
    }
}
