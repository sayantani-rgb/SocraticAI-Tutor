package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavItem
import com.example.ui.theme.EditorialMuted
import com.example.ui.theme.EditorialOutline
import com.example.ui.theme.EditorialPrimary
import com.example.ui.theme.EditorialPrimaryContainer

@Composable
fun SocraticBottomNav(
  currentNav: NavItem,
  onNavigate: (NavItem) -> Unit,
  modifier: Modifier = Modifier
) {
  val outlineColor = MaterialTheme.colorScheme.outline

  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 0.dp,
    modifier = modifier
      .drawBehind {
        // Subtle top border matching Editorial HTML border-t border-[#E1E5DC]
        drawLine(
          color = outlineColor,
          start = Offset(0f, 0f),
          end = Offset(size.width, 0f),
          strokeWidth = 1.dp.toPx()
        )
      }
      .windowInsetsPadding(WindowInsets.navigationBars)
  ) {
    val bottomNavItems = listOf(
      NavItem.HOME,
      NavItem.AI_TUTOR,
      NavItem.SUBJECTS,
      NavItem.QUIZ,
      NavItem.PROGRESS,
      NavItem.SETTINGS
    )

    bottomNavItems.forEach { item ->
      val isSelected = currentNav == item
      NavigationBarItem(
        selected = isSelected,
        onClick = { onNavigate(item) },
        icon = {
          Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.title
          )
        },
        label = {
          Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
              fontSize = 10.sp,
              letterSpacing = 0.4.sp
            )
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = EditorialPrimary,
          selectedTextColor = EditorialPrimary,
          indicatorColor = EditorialPrimaryContainer,
          unselectedIconColor = EditorialMuted,
          unselectedTextColor = EditorialMuted
        ),
        modifier = Modifier.testTag("bottom_nav_${item.route}")
      )
    }
  }
}

