package com.stand.sounder_app.ui.screens.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stand.sounder_app.ui.components.EmptyState
import com.stand.sounder_app.ui.components.LoadingSkeleton
import com.stand.sounder_app.ui.components.ShopCard
import com.stand.sounder_app.ui.screens.shop.ShopHeader
import androidx.compose.ui.res.painterResource
import com.stand.sounder_app.R
import com.stand.sounder_app.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onResourceClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: ShopViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            if (uiState.isLoading || uiState.resources.isEmpty()) false
            else {
                val info = listState.layoutInfo.visibleItemsInfo
                if (info.isEmpty()) false
                else info.last().index >= listState.layoutInfo.totalItemsCount - 2
            }
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.hasMore && !uiState.isLoadingMore) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ShopHeader(onSearchClick = onSearchClick)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        LoadingSkeleton(
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    uiState.error != null && uiState.resources.isEmpty() -> {
                        EmptyState(
                            modifier = Modifier.padding(bottom = 72.dp),
                            isError = true,
                            title = uiState.error ?: stringResource(R.string.load_failed),
                            onRetry = { viewModel.loadResources() }
                        )
                    }

                    uiState.resources.isEmpty() -> {
                        EmptyState(
                            modifier = Modifier.padding(bottom = 72.dp),
                            title = stringResource(R.string.shop_empty),
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_music),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 80.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        )
                    }

                    else -> {
                        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                            isRefreshing = uiState.isLoading,
                            onRefresh = { viewModel.loadResources() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(bottom = 72.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item { Spacer(Modifier.height(4.dp)) }
                                items(uiState.resources, key = { it.id }) { resource ->
                                    ShopCard(
                                        resource = resource,
                                        isInstalled = resource.id in uiState.installedIds,
                                        resumable = resource.id in uiState.pendingIds,
                                        downloadState = uiState.downloadStates[resource.id],
                                        onCardClick = { onResourceClick(resource.id) },
                                        onToggleInstall = { viewModel.toggleInstall(resource) }
                                    )
                                }
                                item {
                                    if (uiState.isLoadingMore) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.size(8.dp))
                                            Text(
                                                text = stringResource(R.string.load_more),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else if (!uiState.hasMore && uiState.resources.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = stringResource(R.string.no_more_data),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                item { Spacer(Modifier.height(8.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
