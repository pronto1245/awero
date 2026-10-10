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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.unit.dp
import app.awero.R

private val SettingsIvory = AweroDesign.ivory
private val SettingsNavy = AweroDesign.navy
private val SettingsCoral = AweroDesign.coral

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val appLocale = context.resources.configuration.locales[0]
    val deviceLanguage = appLocale.getDisplayName(appLocale)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsIvory)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AweroDesign.pagePadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(AweroDesign.sectionSpacing)
    ) {
        Text("AWERO", color = SettingsNavy, style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.settings_title),
            color = SettingsNavy,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(AweroDesign.cardCorner)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
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
            shape = RoundedCornerShape(AweroDesign.cardCorner)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                    shape = RoundedCornerShape(AweroDesign.controlCorner)
                ) {
                    Text(stringResource(R.string.settings_open_system_settings), color = Color.White)
                }
            }
        }
    }
}
