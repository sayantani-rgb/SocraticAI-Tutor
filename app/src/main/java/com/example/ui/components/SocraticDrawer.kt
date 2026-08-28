package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavItem
import com.example.model.Subject
import com.example.ui.theme.EditorialMuted
import com.example.ui.theme.EditorialPrimary
import com.example.ui.theme.EditorialPrimaryContainer

@Composable
fun SocraticDrawerContent(
  currentNav: NavItem,
  onNavigate: (NavItem) -> Unit,
  onSelectHistoryTopic: (String, Subject) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier
) {
  val recentTopics = listOf(
    Pair("Why does ice float on water?", Subject.PHYSICS),
    Pair("Negative times negative intuition", Subject.MATHEMATICS),
    Pair("What is a Python function?", Subject.COMPUTER_SCIENCE)
  )

  ModalDrawerSheet(
    modifier = modifier.widthIn(max = 320.dp),
    drawerContainerColor = MaterialTheme.colorScheme.background,
    drawerTonalElevation = 0.dp,
    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxHeight()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
      // Header brand section (Editorial style)
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(EditorialPrimary),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Socratic Tutor Logo",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
              )
            }

            Column {
              Text(
                text = "Socratic",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = (-0.3).sp
                ),
                color = EditorialPrimary
              )
              Text(
                text = "AI Socratic Tutor",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "“Don't just find the answer. Discover it.”",
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Serif,
              fontStyle = FontStyle.Italic,
              fontWeight = FontWeight.Normal,
              fontSize = 13.sp,
              lineHeight = 18.sp
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = "NAVIGATION",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.4.sp
        ),
        color = EditorialMuted,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Navigation Items List
      Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        NavItem.values().forEach { item ->
          val isSelected = item == currentNav
          NavigationDrawerItem(
            label = {
              Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
              )
            },
            icon = {
              Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.title,
                tint = if (isSelected) EditorialPrimary else EditorialMuted
              )
            },
            badge = {
              if (item.badgeCount != null) {
                Badge(
                  containerColor = EditorialPrimaryContainer,
                  contentColor = EditorialPrimary
                ) {
                  Text(
                    text = item.badgeCount.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                  )
                }
              }
            },
            selected = isSelected,
            onClick = { onNavigate(item) },
            shape = RoundedCornerShape(14.dp),
            colors = NavigationDrawerItemDefaults.colors(
              selectedContainerColor = EditorialPrimaryContainer,
              unselectedContainerColor = Color.Transparent,
              selectedTextColor = EditorialPrimary,
              unselectedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("nav_drawer_${item.route}")
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Recent Inquiries Section
      Text(
        text = "RECENT INQUIRIES",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.4.sp
        ),
        color = EditorialMuted,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
      )

      Spacer(modifier = Modifier.height(6.dp))

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        recentTopics.forEach { (topic, subject) ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectHistoryTopic(topic, subject) }
              .testTag("drawer_history_${subject.name.lowercase()}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = subject.accentColor,
                modifier = Modifier.size(16.dp)
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = topic,
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = subject.displayName,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = subject.accentColor
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Bottom Streak Card
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(EditorialPrimaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.LocalFireDepartment,
              contentDescription = "Streak",
              tint = EditorialPrimary,
              modifier = Modifier.size(20.dp)
            )
          }

          Column {
            Text(
              text = "3-Day Inquiry Streak",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "14 concepts explored",
              style = MaterialTheme.typography.labelSmall,
              color = EditorialMuted
            )
          }
        }
      }
    }
  }
}


