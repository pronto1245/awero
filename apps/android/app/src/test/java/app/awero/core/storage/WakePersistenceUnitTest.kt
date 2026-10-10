package app.awero.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WakePersistenceUnitTest {
    @Test
    fun migrationsReachVersionSix() {
        assertEquals(2, AweroDatabase.MIGRATION_1_2.endVersion)
        assertTrue(AweroDatabase.MIGRATION_1_2.startVersion == 1)
        assertEquals(3, AweroDatabase.MIGRATION_2_3.endVersion)
        assertEquals(2, AweroDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, AweroDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, AweroDatabase.MIGRATION_3_4.endVersion)
        assertEquals(4, AweroDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, AweroDatabase.MIGRATION_4_5.endVersion)
        assertEquals(5, AweroDatabase.MIGRATION_5_6.startVersion)
        assertEquals(6, AweroDatabase.MIGRATION_5_6.endVersion)
    }
}
