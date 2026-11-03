package com.thomas.qwirkle.ui.screens.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thomas.qwirkle.R
import com.thomas.qwirkle.ui.components.BackButton
import com.thomas.qwirkle.ui.components.screenPadding

/** Info über die App, Datenschutz-Hinweis, Spielregeln und Lizenzen. */
@Composable
fun InfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = onBack)
                Text(
                    stringResource(R.string.info_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoCard(stringResource(R.string.info_about_title)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.home_version, version))
                    Text(stringResource(R.string.home_author, stringResource(R.string.author_name)))
                    Text(stringResource(R.string.info_about_text))
                }

                InfoCard(stringResource(R.string.info_privacy_title)) {
                    Text(stringResource(R.string.info_privacy_text))
                }

                InfoCard(stringResource(R.string.info_rules_title)) {
                    Text(
                        stringResource(R.string.info_rules_intro),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Heading(R.string.info_rules_turn_title)
                    Text(stringResource(R.string.info_rules_turn_text))
                    Heading(R.string.info_rules_points_title)
                    Text(stringResource(R.string.info_rules_points_text))
                    Heading(R.string.info_rules_end_title)
                    Text(stringResource(R.string.info_rules_end_text))
                    Heading(R.string.info_rules_app_title)
                    Text(stringResource(R.string.info_rules_app_text))
                    TextButton(onClick = {
                        runCatching { uriHandler.openUri(context.getString(R.string.wikipedia_url)) }
                    }) { Text(stringResource(R.string.home_wikipedia)) }
                    Text(
                        stringResource(R.string.info_rules_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                InfoCard(stringResource(R.string.info_licenses_title)) {
                    Text(stringResource(R.string.info_licenses_text))
                }
            }
        }
    }
}

@Composable
private fun Heading(textRes: Int) {
    Text(
        stringResource(textRes),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}
