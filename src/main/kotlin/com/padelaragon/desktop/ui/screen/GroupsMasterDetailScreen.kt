package com.padelaragon.desktop.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.padelaragon.desktop.data.model.Gender
import com.padelaragon.desktop.data.model.LeagueGroup
import com.padelaragon.desktop.ui.components.LoadingErrorWrapper
import com.padelaragon.desktop.ui.viewmodel.GroupDetailViewModel
import com.padelaragon.desktop.ui.viewmodel.GroupDetailViewModelFactory
import com.padelaragon.desktop.ui.viewmodel.GroupListViewModel
import com.padelaragon.desktop.ui.viewmodel.GroupListViewModelFactory

/**
 * Desktop-only master-detail layout: a persistent left column listing all
 * league categories/groups, and a main panel on the right showing the
 * currently-selected group's detail (standings/results). Selecting a
 * different group in the sidebar just swaps the main panel content, it
 * never navigates away from this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsMasterDetailScreen(
    onTeamClick: (teamId: Int, teamName: String, groupId: Int) -> Unit,
    listViewModelFactory: GroupListViewModelFactory,
    groupDetailViewModelFactory: (groupId: Int, groupName: String) -> GroupDetailViewModelFactory,
    title: String = "Liga de Aragón 2026",
    onBack: (() -> Unit)? = null,
    viewModelKey: String? = null,
    listViewModel: GroupListViewModel = viewModel(key = viewModelKey, factory = listViewModelFactory)
) {
    val uiState by listViewModel.uiState.collectAsState()
    val isRefreshing by listViewModel.isRefreshing.collectAsState()
    var selected by remember { androidx.compose.runtime.mutableStateOf<LeagueGroup?>(null) }
    var detailRefresh: (() -> Unit)? by remember { androidx.compose.runtime.mutableStateOf(null) }

    // Default to the first available group (favorites first) once groups load.
    androidx.compose.runtime.LaunchedEffect(uiState.groups, uiState.favoriteIds) {
        if (selected == null && uiState.groups.isNotEmpty()) {
            val favorite = uiState.groups.firstOrNull { it.id in uiState.favoriteIds }
            selected = favorite ?: uiState.groups.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = title, color = MaterialTheme.colorScheme.onPrimaryContainer)
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver a ligas",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            listViewModel.refresh()
                            detailRefresh?.invoke()
                        },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.width(20.dp).height(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Actualizar",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Left sidebar: category/group list.
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                LoadingErrorWrapper(
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onRetry = listViewModel::retry,
                    modifier = Modifier.fillMaxSize()
                ) {
                val grouped = uiState.groups.groupBy { it.gender }
                val favoriteGroups = uiState.groups.filter { it.id in uiState.favoriteIds }
                val masculineGroups = grouped[Gender.MASCULINA].orEmpty()
                val feminineGroups = grouped[Gender.FEMENINA].orEmpty()
                val sidebarListState = rememberLazyListState()

                Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = sidebarListState,
                    modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (favoriteGroups.isNotEmpty()) {
                        item(key = "fav_header") {
                            SidebarSectionHeader(title = "⭐ FAVORITOS")
                        }
                        items(favoriteGroups, key = { "fav_${it.id}" }) { group ->
                            SidebarGroupItem(
                                group = group,
                                isSelected = group.id == selected?.id,
                                onClick = { selected = group }
                            )
                        }
                    }

                    if (masculineGroups.isNotEmpty()) {
                        item { SidebarSectionHeader(title = "MASCULINA") }
                        items(masculineGroups, key = { it.id }) { group ->
                            SidebarGroupItem(
                                group = group,
                                isSelected = group.id == selected?.id,
                                onClick = { selected = group }
                            )
                        }
                    }

                    if (feminineGroups.isNotEmpty()) {
                        item { SidebarSectionHeader(title = "FEMENINA") }
                        items(feminineGroups, key = { it.id }) { group ->
                            SidebarGroupItem(
                                group = group,
                                isSelected = group.id == selected?.id,
                                onClick = { selected = group }
                            )
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(sidebarListState),
                    style = com.padelaragon.desktop.ui.components.visibleScrollbarStyle()
                )
                }
                }
            }

            Divider(
                modifier = Modifier.fillMaxHeight().width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Main panel: selected group's detail.
            Box(modifier = Modifier.fillMaxSize()) {
                val currentGroup = selected
                if (currentGroup == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Selecciona una categoría",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    key(currentGroup.id) {
                        GroupDetailPanel(
                            groupId = currentGroup.id,
                            groupName = currentGroup.name,
                            onTeamClick = onTeamClick,
                            viewModelFactory = groupDetailViewModelFactory(currentGroup.id, currentGroup.name),
                            onRefreshAvailable = { refreshFn -> detailRefresh = refreshFn }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupDetailPanel(
    groupId: Int,
    groupName: String,
    onTeamClick: (teamId: Int, teamName: String, groupId: Int) -> Unit,
    viewModelFactory: GroupDetailViewModelFactory,
    onRefreshAvailable: (() -> Unit) -> Unit = {},
    viewModel: GroupDetailViewModel = viewModel(
        key = "group_detail_$groupId",
        factory = viewModelFactory
    )
) {
    val isFavorite by viewModel.isFavorite.collectAsState()

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        onRefreshAvailable(viewModel::refresh)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.toggleFavorite() }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                        tint = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            GroupDetailContent(
                groupId = groupId,
                onTeamClick = onTeamClick,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SidebarSectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun SidebarGroupItem(
    group: LeagueGroup,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = group.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
