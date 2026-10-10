package app.awero.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.awero.MainActivity
import app.awero.R
import org.junit.BeforeClass
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
        compose.onNodeWithText(continueLabel).assertIsDisplayed().assert(hasClickAction())
        compose.onNodeWithText(continueLabel).performClick()
        compose.onNodeWithText(context.getString(R.string.create_title)).assertIsDisplayed()
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun resetFirstRunState() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.getSharedPreferences("awero.onboarding", 0).edit().clear().commit()
        }
    }
}
