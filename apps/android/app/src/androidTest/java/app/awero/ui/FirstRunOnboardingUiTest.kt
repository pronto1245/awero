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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
        continueButton.assertIsDisplayed().assert(hasClickAction())
        continueButton.performClick()
        compose.onNodeWithText(context.getString(R.string.create_title)).assertIsDisplayed()
        captureVisual("CreateLargeText")
        setFontScale(1.0f)
        compose.waitUntil(timeoutMillis = 5_000) { context.resources.configuration.fontScale < 1.1f }
        compose.onNodeWithText(context.getString(R.string.create_title)).assertIsDisplayed()
        captureVisual("Create")
        compose.onNodeWithText(context.getString(R.string.create_mission_math_body)).performScrollTo().assertIsDisplayed()
        captureVisual("CreateMissions")
        compose.onNodeWithText(context.getString(R.string.create_mission_qr_body)).performScrollTo().assertIsDisplayed()
        captureVisual("CreateQR")
        compose.onNodeWithText(context.getString(R.string.create_done))
            .assertIsDisplayed()
            .performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(context.getString(R.string.home_alarms))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(context.getString(R.string.home_alarms)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.home_next_alarm), substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        setFontScale(1.8f)
        compose.waitUntil(timeoutMillis = 5_000) { context.resources.configuration.fontScale > 1.5f }
        compose.onNodeWithText(context.getString(R.string.home_add_alarm)).assertIsDisplayed()
        captureVisual("HomeLargeText")
        setFontScale(1.0f)
        compose.waitUntil(timeoutMillis = 5_000) { context.resources.configuration.fontScale < 1.1f }
        captureVisual("Home")
        compose.onNodeWithText(context.getString(R.string.nav_progress)).performClick()
        compose.onNodeWithText(context.getString(R.string.progress_empty_title)).assertIsDisplayed()
        captureVisual("Progress")
        compose.onNodeWithText(context.getString(R.string.nav_profile)).performClick()
        compose.onNodeWithTag("profile.title").assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.profile_settings_hint)).performClick()
        compose.onNodeWithText(context.getString(R.string.settings_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.settings_back), substring = true).performClick()
        captureVisual("Profile")
        if (Build.VERSION.SDK_INT >= 33) {
            val manager = context.getSystemService(android.app.LocaleManager::class.java)
            val previous = manager.applicationLocales
            try {
                for (language in listOf("en", "ru", "pt-BR", "fr", "de", "es")) {
                    manager.applicationLocales = android.os.LocaleList.forLanguageTags(language)
                    compose.waitUntil(timeoutMillis = 10_000) {
                        compose.activity.resources.configuration.locales[0].toLanguageTag() == language
                    }
                    fun localized(id: Int) = compose.activity.getString(id)
                    compose.onNodeWithText(localized(R.string.nav_home)).performClick()
                    compose.onNodeWithText(localized(R.string.home_add_alarm)).assertIsDisplayed()
                    captureVisual("Home-$language")
                    compose.onNodeWithText(localized(R.string.nav_progress)).performClick()
                    compose.onNodeWithText(localized(R.string.progress_empty_title)).assertIsDisplayed()
                    captureVisual("Progress-$language")
                    compose.onNodeWithText(localized(R.string.nav_profile)).performClick()
                    compose.onNodeWithTag("profile.title").assertIsDisplayed()
                    compose.onNodeWithContentDescription(localized(R.string.profile_settings_hint)).performClick()
                    compose.onNodeWithText(localized(R.string.settings_title)).assertIsDisplayed()
                    captureVisual("Settings-$language")
                    compose.onNodeWithText(localized(R.string.settings_back), substring = true).performClick()
                    captureVisual("Profile-$language")
                    compose.onNodeWithText(localized(R.string.nav_home)).performClick()
                    compose.onNodeWithText(localized(R.string.home_add_alarm)).performClick()
                    compose.onNodeWithText(localized(R.string.create_title)).assertIsDisplayed()
                    captureVisual("Create-$language")
                    compose.onNodeWithText(localized(R.string.create_mission_math_body)).performScrollTo().assertIsDisplayed()
                    captureVisual("CreateMissions-$language")
                    compose.onNodeWithText(localized(R.string.create_mission_qr_body)).performScrollTo().assertIsDisplayed()
                    captureVisual("CreateQR-$language")
                    compose.onNodeWithContentDescription(localized(R.string.create_cancel)).performClick()
                }
            } finally {
                manager.applicationLocales = previous
            }
        }
    }

    private fun captureVisual(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val directory = java.io.File(instrumentation.targetContext.cacheDir, "awero-visual").apply { mkdirs() }
        java.io.File(directory, "$name.png").outputStream().use { output ->
            checkNotNull(instrumentation.uiAutomation.takeScreenshot()).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
        }
        if (Build.VERSION.SDK_INT >= 31) {
            val bytes = java.io.File(directory, "$name.png").readBytes()
            val descriptors = instrumentation.uiAutomation.executeShellCommandRw(
                "dd of=/data/local/tmp/awero-visual-$name.png"
            )
            android.os.ParcelFileDescriptor.AutoCloseOutputStream(descriptors[1]).use { it.write(bytes) }
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptors[0]).use { it.readBytes() }
            val saved = instrumentation.uiAutomation.executeShellCommand("cat /data/local/tmp/awero-visual-$name.png")
            val actual = android.os.ParcelFileDescriptor.AutoCloseInputStream(saved).use { it.readBytes() }
            check(bytes.contentEquals(actual)) { "Screenshot export did not preserve PNG bytes: $name" }
        }
    }

    private fun setFontScale(scale: Float) {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("settings put system font_scale $scale")
        FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
        descriptor.close()
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
