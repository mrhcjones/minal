package net.primal.android.core.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import net.primal.android.attachments.domain.CdnImage
import net.primal.android.theme.AppTheme

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@ExperimentalMaterial3Api
@Composable
fun PrimalTopAppBar(
    title: String,
    onNavigationIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    textColor: Color = LocalContentColor.current,
    navigationIcon: ImageVector? = null,
    navigationIconTintColor: Color = LocalContentColor.current,
    autoCloseKeyboardOnNavigationIconClick: Boolean = true,
    avatarCdnImage: CdnImage? = null,
    actions: @Composable RowScope.() -> Unit = {},
    showDivider: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onTitleLongClick: (() -> Unit)? = null,
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = AppTheme.colorScheme.surface,
        scrolledContainerColor = AppTheme.colorScheme.surface,
    ),
    footer: @Composable () -> Unit = {},
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = modifier,
    ) {
        CenterAlignedTopAppBar(
            navigationIcon = {
                if (avatarCdnImage != null) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clip(CircleShape),
                    ) {
                        AvatarThumbnail(
                            avatarCdnImage = avatarCdnImage,
                            modifier = Modifier.size(32.dp),
                            onClick = onNavigationIconClick,
                        )
                    }
                } else if (navigationIcon != null) {
                    AppBarIcon(
                        icon = navigationIcon,
                        onClick = {
                            if (autoCloseKeyboardOnNavigationIconClick) {
                                keyboardController?.hide()
                            }
                            onNavigationIconClick()
                        },
                        tint = navigationIconTintColor,
                    )
                }
            },
            title = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {},
                            onLongClick = onTitleLongClick,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                ) {
                    Text(
                        text = title,
                        color = textColor,
                    )
                    if (subtitle?.isNotBlank() == true) {
                        Text(
                            text = subtitle,
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.extraColorScheme.onSurfaceVariantAlt3,
                        )
                    }
                }
            },
            actions = actions,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )

        Box(
            modifier = Modifier
                .background(color = AppTheme.colorScheme.surface)
                .fillMaxWidth(),
        ) {
            footer()
        }

        if (showDivider) {
            PrimalDivider()
        }
    }
}
