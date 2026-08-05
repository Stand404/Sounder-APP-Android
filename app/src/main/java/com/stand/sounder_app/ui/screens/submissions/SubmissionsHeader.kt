package com.stand.sounder_app.ui.screens.submissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stand.sounder_app.R
import com.stand.sounder_app.data.model.SubmissionStatus
import com.stand.sounder_app.ui.components.PillOption
import com.stand.sounder_app.ui.components.PillToggle
import com.stand.sounder_app.ui.components.SearchBox

@Composable
fun SubmissionsHeader(
    statusOptions: List<PillOption<SubmissionStatus>>,
    selectedStatus: SubmissionStatus,
    tabIndicatorColors: List<Color>,
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onStatusChange: (SubmissionStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.submissions_header_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.submissions_header_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Button(
                onClick = onSubmitClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.submissions_new_btn))
            }
        }

        SearchBox(
            value = keyword,
            onValueChange = onKeywordChange,
            placeholder = stringResource(R.string.submission_search_placeholder)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillToggle(
                options = statusOptions,
                selectedValue = selectedStatus,
                onSelect = onStatusChange,
                indicatorColors = tabIndicatorColors,
                modifier = Modifier.width(300.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}
