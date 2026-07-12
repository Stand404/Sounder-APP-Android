package com.stand.sounder_app.ui.screens.submissions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stand.sounder_app.R
import com.stand.sounder_app.ui.screens.submissions.SubmissionFormHeader
import com.stand.sounder_app.viewmodel.SubmissionFormViewModel
import com.stand.sounder_app.viewmodel.SUBMISSION_IMAGE_SOURCES
import com.stand.sounder_app.viewmodel.SUBMISSION_PLATFORMS
import com.stand.sounder_app.viewmodel.SUBMISSION_VOICE_SOURCES

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubmissionFormScreen(
    onBack: () -> Unit,
    viewModel: SubmissionFormViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                if (state.submitError != null) {
                    Text(
                        text = state.submitError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Button(
                    onClick = { viewModel.submit(onSuccess = onBack) },
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.submission_form_submit))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SubmissionFormHeader(onBack = onBack)
            // 顶部提示框
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        stringResource(R.string.submission_form_info_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            RealTextField(
                label = stringResource(R.string.submission_form_resource_name),
                required = true,
                value = state.resourceName,
                error = state.fieldErrors["resourceName"],
                placeholder = stringResource(R.string.submission_form_resource_name_placeholder),
                maxLength = 10,
                onValueChange = { viewModel.update { s -> s.copy(resourceName = it) } }
            )
            RealTextField(
                label = stringResource(R.string.submission_form_app_name),
                required = true,
                value = state.appName,
                error = state.fieldErrors["appName"],
                placeholder = stringResource(R.string.submission_form_app_name_placeholder),
                maxLength = 10,
                onValueChange = { viewModel.update { s -> s.copy(appName = it) } }
            )
            RealTextField(
                label = stringResource(R.string.submission_form_brief),
                required = true,
                value = state.brief,
                error = state.fieldErrors["brief"],
                placeholder = stringResource(R.string.submission_form_brief_placeholder),
                maxLength = 50,
                singleLine = false,
                onValueChange = { viewModel.update { s -> s.copy(brief = it) } }
            )

            ChipSelector(
                label = stringResource(R.string.submission_form_platform),
                required = true,
                options = SUBMISSION_PLATFORMS,
                selected = state.platform,
                error = state.fieldErrors["platform"],
                onSelected = { viewModel.update { s -> s.copy(platform = it) } }
            )
            RealTextField(
                label = stringResource(R.string.submission_form_platform_id),
                value = state.platformId,
                error = state.fieldErrors["platformId"],
                placeholder = stringResource(R.string.submission_form_platform_id_placeholder),
                onValueChange = { viewModel.update { s -> s.copy(platformId = it) } }
            )
            RealTextField(
                label = stringResource(R.string.submission_form_nickname),
                required = true,
                value = state.nickname,
                error = state.fieldErrors["nickname"],
                placeholder = stringResource(R.string.submission_form_nickname_placeholder),
                maxLength = 50,
                onValueChange = { viewModel.update { s -> s.copy(nickname = it) } }
            )

            // 分隔线 + 来源信息标题
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(top = 4.dp)
            )
            Text(
                stringResource(R.string.submission_form_source_section),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            ChipSelector(
                label = stringResource(R.string.submission_form_image_source),
                required = true,
                options = SUBMISSION_IMAGE_SOURCES,
                selected = state.imageSource,
                error = state.fieldErrors["imageSource"],
                onSelected = { viewModel.update { s -> s.copy(imageSource = it) } }
            )
            if (state.imageSource.isNotBlank()) {
                RealTextField(
                    label = stringResource(R.string.submission_form_image_source_link),
                    value = state.imageSourceLink,
                    error = state.fieldErrors["imageSourceLink"],
                    placeholder = stringResource(R.string.submission_form_image_source_link_placeholder),
                    onValueChange = { viewModel.update { s -> s.copy(imageSourceLink = it) } }
                )
            }
            ChipSelector(
                label = stringResource(R.string.submission_form_voice_source),
                required = true,
                options = SUBMISSION_VOICE_SOURCES,
                selected = state.voiceSource,
                error = state.fieldErrors["voiceSource"],
                onSelected = { viewModel.update { s -> s.copy(voiceSource = it) } }
            )
            if (state.voiceSource.isNotBlank()) {
                RealTextField(
                    label = stringResource(R.string.submission_form_voice_source_link),
                    value = state.voiceSourceLink,
                    error = state.fieldErrors["voiceSourceLink"],
                    placeholder = stringResource(R.string.submission_form_voice_source_link_placeholder),
                    onValueChange = { viewModel.update { s -> s.copy(voiceSourceLink = it) } }
                )
            }

            RealTextField(
                label = stringResource(R.string.submission_form_notes),
                value = state.fileLink,
                error = state.fieldErrors["fileLink"],
                placeholder = stringResource(R.string.submission_form_notes_placeholder),
                singleLine = false,
                onValueChange = { viewModel.update { s -> s.copy(fileLink = it) } }
            )
            if (state.fileLink.isBlank()) {
                Text(
                    text = stringResource(R.string.submission_form_notes_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RealTextField(
    label: String,
    required: Boolean = false,
    value: String,
    error: String?,
    placeholder: String = "",
    maxLength: Int = Int.MAX_VALUE,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label + if (required) " *" else "",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (maxLength != Int.MAX_VALUE) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${value.length}/$maxLength",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            singleLine = singleLine,
            placeholder = {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            isError = error != null,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSelector(
    label: String,
    required: Boolean = false,
    options: List<String>,
    selected: String,
    error: String?,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label + if (required) " *" else "",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { onSelected(option) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        option,
                        fontSize = 13.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // 已选择徽标
        if (selected.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        stringResource(R.string.submission_form_selected, selected),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
