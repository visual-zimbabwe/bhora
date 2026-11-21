package com.bhora.app.ui.matchday

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bhora.app.domain.model.MatchFilter
import com.bhora.app.domain.model.ServiceFilter
import com.bhora.app.ui.theme.BhoraColors
import com.bhora.app.ui.theme.BhoraTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchdayScreen(
    uiState: MatchdayUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (MatchFilter) -> Unit,
    onServiceFilterSelected: (ServiceFilter) -> Unit,
    onToggleFixtureExpanded: (String) -> Unit,
    onPullToRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullState = rememberPullToRefreshState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BhoraColors.Surface)
    ) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onPullToRefresh,
            state = pullState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = uiState.isRefreshing,
                    containerColor = BhoraColors.SurfaceContainer,
                    color = BhoraColors.Secondary,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 64.dp)
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = statusBarTop + 72.dp,
                    bottom = 48.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item(key = "filter_bar") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterToggleRow(
                            activeFilter = uiState.activeFilter,
                            onFilterSelected = onFilterSelected
                        )
                        ServiceFilterRow(
                            activeServiceFilter = uiState.serviceFilter,
                            onServiceFilterSelected = onServiceFilterSelected
                        )
                    }
                }

                if (uiState.noticeMessage != null) {
                    item(key = "notice") {
                        NoticeBanner(
                            message = uiState.noticeMessage,
                            onRetry = onRetry,
                            modifier = Modifier.padding(bottom = 28.dp)
                        )
                    }
                }

                if (uiState.isInitialLoading && uiState.dayGroups.isEmpty()) {
                    item(key = "loading") {
                        EditorialLoadingState(
                            modifier = Modifier.padding(vertical = 56.dp)
                        )
                    }
                } else if (uiState.dayGroups.isEmpty()) {
                    item(key = "empty") {
                        EditorialEmptyState(
                            activeFilter = uiState.activeFilter,
                            serviceFilter = uiState.serviceFilter,
                            searchQuery = uiState.searchQuery,
                            totalPrimeFixtureCount = uiState.totalPrimeFixtureCount,
                            onRefresh = onRetry,
                            modifier = Modifier.padding(vertical = 48.dp)
                        )
                    }
                } else {
                    items(
                        items = uiState.dayGroups,
                        key = { group -> group.date.toString() }
                    ) { group ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = group.dateLabel,
                                style = BhoraTextStyles.StatusEditorial,
                                color = BhoraColors.OnSurfaceVariant.copy(alpha = 0.50f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp)
                            )

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = BhoraColors.SurfaceVariant.copy(alpha = 0.30f),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                group.fixtures.forEach { fixture ->
                                    FixtureRow(
                                        fixture = fixture,
                                        isExpanded = fixture.id in uiState.expandedFixtureIds,
                                        onToggleExpand = { onToggleFixtureExpanded(fixture.id) },
                                        zoneId = uiState.zoneId
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        MatchdayHeader(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun MatchdayHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BhoraColors.Surface.copy(alpha = 0.88f))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bhora",
                style = BhoraTextStyles.DisplayBrand,
                color = BhoraColors.Secondary,
                maxLines = 1
            )

            Box(
                modifier = Modifier.width(168.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "SEARCH CLUB",
                        style = BhoraTextStyles.KickoffTime,
                        color = BhoraColors.OnSurfaceVariant.copy(alpha = 0.40f),
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { value -> onSearchQueryChange(value.uppercase()) },
                    singleLine = true,
                    textStyle = BhoraTextStyles.KickoffTime.copy(
                        color = BhoraColors.OnSurface,
                        textAlign = TextAlign.End
                    ),
                    cursorBrush = SolidColor(BhoraColors.Secondary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FilterToggleRow(
    activeFilter: MatchFilter,
    onFilterSelected: (MatchFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorialTextTrigger(
            label = "ALL",
            selected = activeFilter == MatchFilter.ALL,
            onClick = { onFilterSelected(MatchFilter.ALL) }
        )

        Text(
            text = "/",
            style = BhoraTextStyles.FilterEditorial,
            color = BhoraColors.OnSurfaceVariant.copy(alpha = 0.40f)
        )

        EditorialTextTrigger(
            label = "LIVE ONLY",
            selected = activeFilter == MatchFilter.LIVE_ONLY,
            onClick = { onFilterSelected(MatchFilter.LIVE_ONLY) }
        )
    }
}

@Composable
private fun ServiceFilterRow(
    activeServiceFilter: ServiceFilter,
    onServiceFilterSelected: (ServiceFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorialTextTrigger(
            label = "ALL",
            selected = activeServiceFilter == ServiceFilter.ALL,
            onClick = { onServiceFilterSelected(ServiceFilter.ALL) }
        )

        Text(
            text = "/",
            style = BhoraTextStyles.FilterEditorial,
            color = BhoraColors.OnSurfaceVariant.copy(alpha = 0.40f)
        )

        EditorialTextTrigger(
            label = "PRIME",
            selected = activeServiceFilter == ServiceFilter.PRIME,
            onClick = { onServiceFilterSelected(ServiceFilter.PRIME) }
        )

        Text(
            text = "/",
            style = BhoraTextStyles.FilterEditorial,
            color = BhoraColors.OnSurfaceVariant.copy(alpha = 0.40f)
        )

        EditorialTextTrigger(
            label = "MAX",
            selected = activeServiceFilter == ServiceFilter.MAX,
            onClick = { onServiceFilterSelected(ServiceFilter.MAX) }
        )
    }
}

@Composable
private fun EditorialTextTrigger(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = label,
        style = BhoraTextStyles.FilterEditorial,
        color = if (selected) {
            BhoraColors.Secondary
        } else {
            BhoraColors.OnSurfaceVariant.copy(alpha = 0.40f)
        },
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
private fun NoticeBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = BhoraTextStyles.RegionEyebrow,
            color = BhoraColors.OnSurfaceVariant.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "RETRY",
            style = BhoraTextStyles.FilterEditorial,
            color = BhoraColors.Secondary.copy(alpha = 0.85f),
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onRetry
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EditorialLoadingState(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loading_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loading_alpha"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SCANNING GLOBAL BROADCASTS",
            style = BhoraTextStyles.StatusEditorial,
            color = BhoraColors.OnSurfaceVariant.copy(alpha = alpha),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EditorialEmptyState(
    activeFilter: MatchFilter,
    serviceFilter: ServiceFilter,
    searchQuery: String,
    totalPrimeFixtureCount: Int,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val serviceLabel = when (serviceFilter) {
        ServiceFilter.ALL -> "PRIME VIDEO OR HBO MAX"
        ServiceFilter.PRIME -> "PRIME VIDEO"
        ServiceFilter.MAX -> "HBO MAX"
    }

    val headline = when {
        searchQuery.isNotBlank() -> "NO FIXTURES FOUND"
        activeFilter == MatchFilter.LIVE_ONLY && totalPrimeFixtureCount > 0 -> "NO LIVE FIXTURES"
        else -> "NO $serviceLabel FIXTURES LISTED"
    }

    val subtitle = when {
        searchQuery.isNotBlank() -> null
        activeFilter == MatchFilter.LIVE_ONLY && totalPrimeFixtureCount > 0 ->
            "Switch to ALL to view upcoming or today's broadcast matches."
        else ->
            "International broadcaster schedules are typically published 3–7 days before kickoff."
    }

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = headline,
            style = BhoraTextStyles.StatusEditorial,
            color = BhoraColors.OnSurfaceVariant.copy(alpha = 0.45f),
            textAlign = TextAlign.Center
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = subtitle,
                style = BhoraTextStyles.BodyMuted,
                color = BhoraColors.OnSurfaceVariant.copy(alpha = 0.40f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (searchQuery.isBlank()) {
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "REFRESH",
                style = BhoraTextStyles.FilterEditorial,
                color = BhoraColors.Secondary.copy(alpha = 0.85f),
                modifier = Modifier
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onRefresh
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
