package app.awero.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.awero.R

private val OnboardingIvory = Color(0xFFFFF8EF)
private val OnboardingNavy = Color(0xFF14294B)
private val OnboardingCoral = Color(0xFFFF684B)

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(OnboardingIvory).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("AWERO", style = MaterialTheme.typography.headlineMedium, color = OnboardingNavy)
            Text(
                stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineLarge,
                color = OnboardingNavy,
                modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(R.string.onboarding_body), color = OnboardingNavy.copy(alpha = .72f))
            Text(stringResource(R.string.onboarding_privacy), color = OnboardingNavy.copy(alpha = .72f))
            Text(stringResource(R.string.onboarding_permissions), color = OnboardingNavy.copy(alpha = .72f))
        }
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OnboardingCoral)
        ) {
            Text(stringResource(R.string.onboarding_create_alarm), color = Color.White)
        }
    }
}
