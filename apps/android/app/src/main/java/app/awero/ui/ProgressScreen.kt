package app.awero.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.awero.R

@Composable
fun AweroProgressScreen(modifier: Modifier = Modifier, onSetAlarm: () -> Unit) {
    val ivory = AweroDesign.ivory
    val navy = AweroDesign.navy
    val coral = AweroDesign.coral

    Column(
        modifier = modifier.fillMaxSize().background(ivory).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("AWERO", color = navy, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.progress_title), color = navy, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(20.dp))
        AweroNavigationIcon("progress")
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.progress_empty_title),
            color = navy,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Text(
                stringResource(R.string.progress_empty_body),
                color = navy.copy(alpha = .72f),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(20.dp)
            )
        }
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onSetAlarm,
            colors = ButtonDefaults.buttonColors(containerColor = coral),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(stringResource(R.string.progress_empty_action), color = AweroDesign.navy)
        }
    }
}
