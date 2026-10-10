package app.awero.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.awero.MainActivity
import app.awero.R
import org.junit.BeforeClass
import org.junit.AfterClass
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
        val onboardingTitle = context.getString(R.string.onboarding_title)
        val continueLabel = context.getString(R.string.onboarding_create_alarm)

        compose.onNodeWithText(onboardingTitle)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val continueButton = compose.onNodeWithText(continueLabel)
        continueButton.performScrollTo().assertIsDisplayed().assert(hasClickAction())
        continueButton.performClick()
        compose.onNodeWithText(context.getString(R.string.create_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.create_save))
            .performScrollTo()
            .performClick()
        compose.onNodeWithText(context.getString(R.string.home_alarms)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.home_empty_title)).assertDoesNotExist()
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun resetFirstRunState() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.getSharedPreferences("awero.onboarding", 0).edit().clear().commit()
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put system font_scale 1.8").close()
        }

        @AfterClass
        @JvmStatic
        fun restoreSystemFontScale() {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put system font_scale 1.0").close()
        }
    }
}
