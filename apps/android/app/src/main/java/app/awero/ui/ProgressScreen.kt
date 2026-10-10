package app.awero.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.awero.R

@Composable
fun AweroProgressScreen(modifier: Modifier = Modifier, onSetAlarm: () -> Unit, onOpenSettings: () -> Unit) {
    val navy = AweroDesign.navy
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize().background(AweroDesign.ivory).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("AWERO", color = navy, style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black), modifier = Modifier.weight(1f))
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp).semantics {
                    contentDescription = context.getString(R.string.settings_title)
                }
            ) { AweroNavigationIcon("settings") }
        }
        Text(
            stringResource(R.string.progress_title), color = navy,
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            modifier = Modifier.semantics { heading() }
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF3E5))
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AweroNavigationIcon("plant", tint = AweroDesign.sage)
                Text(stringResource(R.string.progress_empty_title), color = navy, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.progress_empty_body), color = navy.copy(alpha = .72f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(
            onClick = onSetAlarm,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AweroDesign.coral),
            shape = RoundedCornerShape(AweroDesign.controlCorner)
        ) {
            Text(stringResource(R.string.progress_empty_action), color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
    }
}
