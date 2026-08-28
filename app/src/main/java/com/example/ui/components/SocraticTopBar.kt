package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavItem
import com.example.model.Subject
import com.example.ui.theme.EditorialMuted
import com.example.ui.theme.EditorialPrimary
import com.example.ui.theme.EditorialSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocraticTopBar(
  currentNav: NavItem,
  selectedSubject: Subject?,
  onOpenDrawer: () -> Unit,
  modifier: Modifier = Modifier
) {
  TopAppBar(
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (currentNav == NavItem.HOME) {
          // Editorial rounded-xl square badge with white school icon
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(EditorialPrimary),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = "Socratic Icon",
              tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }
          Column {
            Text(
              text = "Socratic",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                fontSize = 20.sp
              ),
              color = EditorialPrimary
            )
            Text(
              text = "AI Socratic Method",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
              ),
              color = EditorialMuted
            )
          }
        } else {
          Text(
            text = currentNav.title,
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = (-0.2).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    },
    navigationIcon = {
      // Circular oat menu button
      IconButton(
        onClick = onOpenDrawer,
        modifier = Modifier
          .padding(start = 6.dp)
          .size(42.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .testTag("top_bar_menu_button")
      ) {
        Icon(
          imageVector = Icons.Default.Menu,
          contentDescription = "Open Sidebar Navigation",
          tint = EditorialPrimary,
          modifier = Modifier.size(22.dp)
        )
      }
    },
    actions = {
      if (selectedSubject != null && currentNav == NavItem.AI_TUTOR) {
        Surface(
          shape = RoundedCornerShape(50),
          color = selectedSubject.pastelBg,
          modifier = Modifier.padding(end = 12.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = selectedSubject.icon,
              contentDescription = selectedSubject.displayName,
              tint = selectedSubject.accentColor,
              modifier = Modifier.size(15.dp)
            )
            Text(
              text = selectedSubject.displayName,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = selectedSubject.accentColor
            )
          }
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.background,
      titleContentColor = MaterialTheme.colorScheme.onSurface
    ),
    modifier = modifier
  )
}

