package com.stand.sounder_app.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stand.sounder_app.MyApp
import com.stand.sounder_app.data.model.RemoteResource
import com.stand.sounder_app.ui.components.LoadingSkeleton
import com.stand.sounder_app.ui.components.ShopCard
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.ui.res.stringResource
import com.stand.sounder_app.R

private const val PREFS_NAME = "sounder_search"
private const val KEY_HISTORY = "search_history"

private fun loadHistory(context: android.content.Context): MutableList<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_HISTORY, null) ?: return mutableListOf()
    return try {
        Gson().fromJson(json, TypeToken.getParameterized(MutableList::class.java, String::class.java).type)
    } catch (_: Exception) {
        mutableListOf()
    }
}

private fun saveHistory(context: android.content.Context, history: List<String>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_HISTORY, Gson().toJson(history)).apply()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onResourceClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { MyApp.instance.resourceRepository }
    val scope = rememberCoroutineScope()

    var keyword by remember { mutableStateOf("") }
    val searchHistory = remember { mutableStateListOf<String>().apply { addAll(loadHistory(context)) } }
    var searchResults by remember { mutableStateOf<List<RemoteResource>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var installedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var downloadStates by remember { mutableStateOf<Map<String, com.stand.sounder_app.data.download.DownloadState>>(emptyMap()) }

    val focusManager = LocalFocusManager.current

    val downloadManager = remember { MyApp.instance.downloadManager }

    LaunchedEffect(Unit) {
        // 监听本地安装状态，排除正在下载中的资源
        repository.getAllLocalResources().collect { localList ->
            val dbIds = localList.map { it.id }.toSet()
            val activeDownloads = downloadManager.getActiveDownloadIds()
            installedIds = dbIds - activeDownloads
        }
    }

    // 监听全局下载状态变化
    LaunchedEffect(Unit) {
        downloadManager.stateChanges.collect { state ->
            downloadStates = downloadStates + (state.resourceId to state)
        }
    }

    fun performSearch(query: String) {
        if (query.isBlank()) return
        focusManager.clearFocus()
        isSearching = true
        hasSearched = true

        if (query !in searchHistory) {
            searchHistory.add(0, query)
            if (searchHistory.size > 10) {
                searchHistory.removeAt(searchHistory.lastIndex)
            }
            saveHistory(context, searchHistory)
        }

        scope.launch {
            val result = repository.searchRemoteResources(query)
            result.fold(
                onSuccess = { list ->
                    searchResults = list
                    // 进入搜索结果后，从全局下载状态快照同步正在下载/暂停资源的进度
                    downloadStates = downloadStates + list.mapNotNull { r ->
                        downloadManager.getDownloadState(r.id)?.let { r.id to it }
                    }.toMap()
                },
                onFailure = {
                    searchResults = emptyList()
                }
            )
            isSearching = false
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SearchHeader(
                keyword = keyword,
                onKeywordChange = { keyword = it },
                onBack = onBack,
                onSearch = { performSearch(keyword) }
            )

            if (!hasSearched) {
                if (searchHistory.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_history),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.search_history),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            searchHistory.clear()
                            saveHistory(context, searchHistory)
                        }) {
                            Text(stringResource(R.string.clear_text), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        searchHistory.forEach { historyItem ->
                            AssistChip(
                                onClick = {
                                    keyword = historyItem
                                    performSearch(historyItem)
                                },
                                label = { Text(historyItem) },
                                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp),
                                border = null,
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_search_results),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            when {
                isSearching -> {
                    LoadingSkeleton(modifier = Modifier.padding(top = 8.dp))
                }
                hasSearched && searchResults.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_search_results),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                hasSearched -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 72.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        items(searchResults, key = { it.id }) { resource ->
                            ShopCard(
                                resource = resource,
                                isInstalled = resource.id in installedIds,
                                downloadState = downloadStates[resource.id],
                                onCardClick = { onResourceClick(resource.id) },
                                onToggleInstall = {
                                    // 统一委托全局 DownloadManager：下载在应用级作用域持续，切页不中断
                                    downloadManager.toggleDownload(resource.id, MyApp.instance.filesDir)
                                }
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }

        // Focus auto-request moved into SearchHeader
    }
}
