package com.bhora.app.ui.matchday

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bhora.app.domain.logic.FixtureTimeFormatter
import com.bhora.app.domain.model.MatchStatus
import com.bhora.app.domain.model.PrimeFixture
import com.bhora.app.ui.theme.BhoraColors
import com.bhora.app.ui.theme.BhoraTextStyles
import java.time.ZoneId

@Composable
fun FixtureRow(
    fixture: PrimeFixture,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault()
) {
    val kickoffFormatted = remember(fixture.kickoffUtcMillis, zoneId) {
        FixtureTimeFormatter.formatKickoffTime(fixture.kickoffUtcMillis, zoneId)
    }

    val combinedNameLength = fixture.homeTeam.length + fixture.awayTeam.length
    val useStackedLayout = combinedNameLength > 34
    val teamTextStyle = if (combinedNameLength > 26) {
        BhoraTextStyles.MatchupTeamCompact
    } else {
        BhoraTextStyles.MatchupTeam
    }

    val statusText = when (fixture.status) {
        MatchStatus.LIVE -> "LIVE"
        MatchStatus.UPCOMING -> "UPCOMING"
        MatchStatus.ENDED -> "ENDED"
    }
    val statusColor = when (fixture.status) {
        MatchStatus.LIVE -> BhoraColors.Secondary
        MatchStatus.UPCOMING -> BhoraColors.OnSurfaceVariant.copy(alpha = 0.40f)
        MatchStatus.ENDED -> BhoraColors.OnSurfaceVariant.copy(alpha = 0.40f)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = if (isExpanded) "Expanded" else "Collapsed"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onToggleExpand
            )
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = fixture.competition.displayLabel,
            style = BhoraTextStyles.RegionEyebrow,
            color = BhoraColors.OnSurfaceVariant.copy(alpha = 0.36f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (useStackedLayout) {
            Text(
                text = fixture.homeTeam,
                style = teamTextStyle,
                color = BhoraColors.OnSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "VS",
                style = BhoraTextStyles.MatchupSeparator,
                color = BhoraColors.OnSurfaceVariant.copy(alpha = 0.40f),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Text(
                text = fixture.awayTeam,
                style = teamTextStyle,
                color = BhoraColors.OnSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fixture.homeTeam,
                    style = teamTextStyle,
                    color = BhoraColors.OnSurface,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "VS",
                    style = BhoraTextStyles.MatchupSeparator,
                    color = BhoraColors.OnSurfaceVariant.copy(alpha = 0.40f),
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = fixture.awayTeam,
                    style = teamTextStyle,
                    color = BhoraColors.OnSurface,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = kickoffFormatted,
            style = BhoraTextStyles.KickoffTime,
            color = BhoraColors.OnSurfaceVariant.copy(alpha = 0.70f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = statusText,
            style = BhoraTextStyles.FilterEditorial,
            color = statusColor,
            textAlign = TextAlign.Center
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(1.dp)
                        .background(BhoraColors.Secondary.copy(alpha = 0.35f))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "BROADCAST REGIONS",
                    style = BhoraTextStyles.RegionEyebrow,
                    color = BhoraColors.Secondary.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    fixture.primeRegions.forEach { region ->
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(color = BhoraColors.OnSurface.copy(alpha = 0.90f))
                                ) {
                                    append(region.country.uppercase())
                                }
                                withStyle(
                                    SpanStyle(color = BhoraColors.OnSurfaceVariant.copy(alpha = 0.35f))
                                ) {
                                    append("  ·  ")
                                }
                                withStyle(
                                    SpanStyle(color = BhoraColors.OnSurfaceVariant.copy(alpha = 0.58f))
                                ) {
                                    append(region.channelName.uppercase())
                                }
                            },
                            style = BhoraTextStyles.RegionEntry,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
