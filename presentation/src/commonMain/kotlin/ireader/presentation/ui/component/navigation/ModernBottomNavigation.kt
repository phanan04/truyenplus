package ireader.presentation.ui.component.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.i18n.resources.*
import ireader.i18n.resources.Res
import ireader.domain.utils.extensions.currentTimeToLong

/**
 * Reference-style floating navigation with equal-width items.
 * Keeps IReader's single/double tap callbacks and selected-tab semantics.
 */
@Composable
fun ModernBottomNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        color = containerColor.copy(alpha = 0.97f),
        contentColor = contentColor,
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun RowScope.ModernNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: Painter,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    alwaysShowLabel: Boolean = true,
    onDoubleClick: (() -> Unit)? = null
) {
    var lastClickTime by remember { mutableStateOf(0L) }
    val handleClick: () -> Unit = remember(onClick, onDoubleClick) {
        {
            val currentTime = currentTimeToLong()
            if (onDoubleClick != null && currentTime - lastClickTime < 300L) {
                onDoubleClick()
                lastClickTime = 0L
            } else {
                onClick()
                lastClickTime = currentTime
            }
        }
    }
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150), label = "navigationTint"
    )
    Column(
        modifier = modifier.weight(1f).fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = selected, enabled = enabled, role = androidx.compose.ui.semantics.Role.Tab, onClick = handleClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painter = icon, contentDescription = null, tint = tint, modifier = Modifier.size(23.dp))
        if (alwaysShowLabel || selected) {
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = tint, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Compact modern navigation item for tablets/navigation rail
 * Supports both single tap and double tap gestures
 */
@Composable
fun ModernNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: Painter,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    alwaysShowLabel: Boolean = true,
    onDoubleClick: (() -> Unit)? = null
) {
    val localizeHelper = requireNotNull(LocalLocalizeHelper.current) { "LocalLocalizeHelper not provided" }
    val animatedScale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = localizeHelper.localize(Res.string.scale)
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    
    // Fast double-tap detection without delaying single tap
    var lastClickTime by remember { mutableStateOf(0L) }
    val doubleTapTimeout = 300L
    
    val handleClick: () -> Unit = remember(onClick, onDoubleClick) {
        {
            val currentTime = currentTimeToLong()
            if (onDoubleClick != null && currentTime - lastClickTime < doubleTapTimeout) {
                // Double tap detected
                onDoubleClick()
                lastClickTime = 0L // Reset to prevent triple-tap
            } else {
                // Single tap - execute immediately
                onClick()
                lastClickTime = currentTime
            }
        }
    }

    Surface(
        modifier = modifier
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = handleClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) primaryColor.copy(alpha = 0.15f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (selected) {
                            Modifier.background(
                                color = primaryColor.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint = if (selected) primaryColor else onSurface.copy(alpha = 0.6f)
                )
            }

            if (alwaysShowLabel || selected) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (selected) primaryColor else onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
