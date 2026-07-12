package com.stand.sounder_app.ui.screens.search

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stand.sounder_app.ui.components.SearchBox
import com.stand.sounder_app.R

@Composable
fun SearchHeader(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.back)
            )
        }
        SearchBox(
            value = keyword,
            onValueChange = onKeywordChange,
            placeholder = stringResource(R.string.search_placeholder),
            onSearch = onSearch,
            showSearchIcon = false,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            horizontalPadding = 0.dp
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = onSearch) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = stringResource(R.string.search)
            )
        }
    }
}
