package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.graphics.vector.ImageVector

enum class Difficulty(
  val displayName: String,
  val subtitle: String,
  val icon: ImageVector,
  val levelBadge: String
) {
  BEGINNER(
    displayName = "Beginner",
    subtitle = "Foundational concepts with gentle, step-by-step guidance",
    icon = Icons.Default.Spa,
    levelBadge = "Lvl 1"
  ),
  INTERMEDIATE(
    displayName = "Intermediate",
    subtitle = "Deeper conceptual challenges and analytical questions",
    icon = Icons.Default.Bolt,
    levelBadge = "Lvl 2"
  ),
  ADVANCED(
    displayName = "Advanced",
    subtitle = "Rigorous Socratic counter-examples and first-principles reasoning",
    icon = Icons.Default.FitnessCenter,
    levelBadge = "Lvl 3"
  )
}
