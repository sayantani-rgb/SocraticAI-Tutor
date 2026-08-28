package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavItem(
  val title: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val badgeCount: Int? = null,
  val route: String
) {
  HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, null, "home"),
  AI_TUTOR("AI Tutor", Icons.Filled.Psychology, Icons.Outlined.Psychology, null, "ai_tutor"),
  SUBJECTS("Subjects", Icons.Filled.School, Icons.Outlined.School, 9, "subjects"),
  QUIZ("Quiz", Icons.Filled.Quiz, Icons.Outlined.Quiz, null, "quiz"),
  PROGRESS("Progress", Icons.AutoMirrored.Filled.TrendingUp, Icons.AutoMirrored.Outlined.TrendingUp, null, "progress"),
  SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, null, "settings")
}
