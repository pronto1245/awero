package app.awero.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.awero.R

@Composable
fun ProfileScreen(modifier: Modifier = Modifier, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxSize().background(AweroDesign.ivory)
            .padding(horizontal = AweroDesign.pagePadding, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(AweroDesign.sectionSpacing)
    ) {
        Text("AWERO", color = AweroDesign.navy, style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black))
        Text(
            stringResource(R.string.nav_profile),
            color = AweroDesign.navy,
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            modifier = Modifier.semantics { heading() }.testTag("profile.title")
        )
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenSettings).semantics {
                contentDescription = context.getString(R.string.profile_settings_hint)
            },
            shape = RoundedCornerShape(AweroDesign.cardCorner),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AweroNavigationIcon("settings", tint = AweroDesign.coral)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(R.string.settings_title), color = AweroDesign.navy, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.profile_settings_hint), color = AweroDesign.navy.copy(alpha = .68f), style = MaterialTheme.typography.bodyMedium)
                }
                Text("›", color = AweroDesign.coral, style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
