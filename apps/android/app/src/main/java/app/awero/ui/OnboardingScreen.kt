package app.awero.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("AWERO", style = MaterialTheme.typography.displaySmall, color = Color.White)
            Text("Wake up. Stay up.", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text("AWERO uses wake-up missions to get you out of bed. Your first alarm works without an account or internet.", color = Color.White.copy(alpha = .7f))
            Text("You can test the alarm before relying on it.", color = Color.White.copy(alpha = .7f))
        }
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Continue")
        }
    }
}
