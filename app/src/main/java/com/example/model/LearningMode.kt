package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector

enum class LearningMode(
  val displayName: String,
  val shortTag: String,
  val description: String,
  val icon: ImageVector,
  val badgeLabel: String
) {
  SOCRATIC(
    displayName = "Socratic",
    shortTag = "Socratic Mode",
    description = "Teaches through guided questioning, leading you to deduce core insights without direct answers.",
    icon = Icons.Default.School,
    badgeLabel = "Guided Questions"
  ),
  HINT(
    displayName = "Hint",
    shortTag = "Hint Mode",
    description = "Provides progressive hints—subtle at first, then increasingly detailed if you need more help.",
    icon = Icons.Default.Lightbulb,
    badgeLabel = "Progressive Hints"
  ),
  EXPLAIN(
    displayName = "Explain",
    shortTag = "Explain Mode",
    description = "Directly explains concepts with clear definitions, real-world analogies, and step-by-step walkthroughs.",
    icon = Icons.Default.AutoAwesome,
    badgeLabel = "Direct Explanations"
  ),
  CHALLENGE(
    displayName = "Challenge",
    shortTag = "Challenge Mode",
    description = "Presents rigorous, deeper problem-solving questions testing edge cases and critical thinking.",
    icon = Icons.Default.Bolt,
    badgeLabel = "Deep Reasoning"
  )
}
