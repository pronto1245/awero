package app.awero.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WakePersistenceUnitTest {
    @Test
    fun migrationsReachVersionFour() {
        assertEquals(2, AweroDatabase.MIGRATION_1_2.endVersion)
        assertTrue(AweroDatabase.MIGRATION_1_2.startVersion == 1)
        assertEquals(3, AweroDatabase.MIGRATION_2_3.endVersion)
        assertEquals(2, AweroDatabase.MIGRATION_2_3.startVersion)
        assertEquals(4, AweroDatabase.MIGRATION_3_4.endVersion)
        assertEquals(3, AweroDatabase.MIGRATION_3_4.startVersion)
    }
}
