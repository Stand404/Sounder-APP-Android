package com.stand.sounder_app.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stand.sounder_app.R

@Composable
fun SettingsHeader(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.tab_settings),
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
