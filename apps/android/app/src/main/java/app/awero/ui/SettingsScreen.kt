package app.awero.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.awero.R
import java.util.Locale

private val SettingsIvory = AweroDesign.ivory
private val SettingsNavy = AweroDesign.navy
private val SettingsCoral = AweroDesign.coral

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appLocale = context.resources.configuration.locales[0]
    val deviceLanguage = appLocale.getDisplayName(appLocale)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsIvory)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = context.getString(R.string.settings_back) }
            ) {
                Text("‹", color = SettingsNavy, style = MaterialTheme.typography.headlineMedium)
            }
            Text(
                stringResource(R.string.settings_title),
                color = SettingsNavy,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.settings_language_title),
                    color = SettingsNavy,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(deviceLanguage, color = SettingsNavy, style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.settings_language_body),
                    color = SettingsNavy.copy(alpha = .68f)
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.settings_alarm_permissions_title),
                    color = SettingsNavy,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(R.string.settings_alarm_permissions_body),
                    color = SettingsNavy.copy(alpha = .68f)
                )
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SettingsCoral),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.settings_open_system_settings), color = AweroDesign.navy)
                }
            }
        }
    }
}
