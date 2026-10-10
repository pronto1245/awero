package app.awero.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.awero.MainActivity
import app.awero.R
import java.io.FileInputStream
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstRunOnboardingUiTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstRunContinuesFromAccessibleOnboardingToAlarmCreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(
            "Notification access must be pregranted for the successful first-run path",
            PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
        )
        assertTrue(
            "Exact alarm access must be pregranted for the successful first-run path",
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        )
        if (Build.VERSION.SDK_INT >= 34) {
            assertTrue(
                "Full-screen alarm access must be pregranted for the successful first-run path",
                context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
            )
        }
        val onboardingTitle = context.getString(R.string.onboarding_title)
        val continueLabel = context.getString(R.string.onboarding_create_alarm)

        compose.onNodeWithText(onboardingTitle)
            .performScrollTo()
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val continueButton = compose.onNodeWithText(continueLabel)
        continueButton.performScrollTo().assertIsDisplayed().assert(hasClickAction())
        continueButton.performClick()
        compose.onNodeWithText(context.getString(R.string.create_title)).assertIsDisplayed()
        captureVisual("Create")
        compose.onNodeWithText(context.getString(R.string.create_save))
            .performScrollTo()
            .performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(context.getString(R.string.home_alarms))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(context.getString(R.string.home_alarms)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.home_next_alarm), substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        captureVisual("HomeLargeText")
        compose.onNodeWithText(context.getString(R.string.nav_progress)).performClick()
        compose.onNodeWithText(context.getString(R.string.progress_empty_title)).assertIsDisplayed()
        captureVisual("Progress")
        compose.onNodeWithText(context.getString(R.string.nav_profile)).performClick()
        compose.onNodeWithText(context.getString(R.string.settings_title)).assertIsDisplayed()
        captureVisual("Profile")
    }

    private fun captureVisual(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val directory = java.io.File(instrumentation.targetContext.cacheDir, "awero-visual").apply { mkdirs() }
        java.io.File(directory, "$name.png").outputStream().use { output ->
            checkNotNull(instrumentation.uiAutomation.takeScreenshot()).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun resetFirstRunState() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.getSharedPreferences("awero.onboarding", 0).edit().clear().commit()
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            listOf(
                "pm grant app.awero android.permission.POST_NOTIFICATIONS",
                "appops set app.awero SCHEDULE_EXACT_ALARM allow",
                "appops set app.awero USE_FULL_SCREEN_INTENT allow",
                "settings put system font_scale 1.8"
            ).forEach { command ->
                val descriptor = uiAutomation.executeShellCommand(command)
                val output = FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
                descriptor.close()
                check(output.isBlank()) { "Emulator setup failed for '$command': $output" }
            }
        }

        @AfterClass
        @JvmStatic
        fun restoreSystemFontScale() {
            val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put system font_scale 1.0")
            FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
            descriptor.close()
        }
    }
}
