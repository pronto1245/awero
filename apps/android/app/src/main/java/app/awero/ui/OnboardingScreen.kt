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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.awero.R

private val OnboardingIvory = AweroDesign.ivory
private val OnboardingNavy = AweroDesign.navy
private val OnboardingCoral = AweroDesign.coral

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(OnboardingIvory).padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AWERO", style = MaterialTheme.typography.headlineSmall, color = OnboardingNavy, modifier = Modifier.weight(1f))
                Text("☀", color = Color(0xFFFFA429), style = MaterialTheme.typography.titleLarge)
            }
            SunriseArtwork(height = 174.dp, rounded = true)
            Text(
                stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineLarge,
                color = OnboardingNavy,
                modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(R.string.onboarding_body), color = OnboardingNavy.copy(alpha = .72f))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.onboarding_privacy), color = OnboardingNavy.copy(alpha = .78f))
                    Text(stringResource(R.string.onboarding_permissions), color = OnboardingNavy.copy(alpha = .72f))
                }
            }
        }
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OnboardingCoral),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
        ) {
            Text(stringResource(R.string.onboarding_create_alarm), color = AweroDesign.navy)
        }
    }
}
