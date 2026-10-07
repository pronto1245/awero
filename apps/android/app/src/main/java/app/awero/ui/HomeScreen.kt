package app.awero.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onCreateAlarm: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("AWERO", style = MaterialTheme.typography.displaySmall, color = Color.White)
        Text("Wake up. Stay up.", color = Color.White.copy(alpha = .6f))
        Spacer(Modifier.weight(1f))
        Button(onClick = onCreateAlarm, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Create alarm")
        }
    }
}
