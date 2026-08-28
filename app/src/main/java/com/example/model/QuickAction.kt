package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector

enum class QuickAction(
  val label: String,
  val promptText: String,
  val icon: ImageVector,
  val testTag: String
) {
  GIVE_HINT(
    label = "Give me a hint",
    promptText = "Give me a hint",
    icon = Icons.Default.Lightbulb,
    testTag = "quick_action_give_me_a_hint"
  ),
  GUIDE_STEP_BY_STEP(
    label = "Guide me step by step",
    promptText = "Guide me step by step",
    icon = Icons.Default.School,
    testTag = "quick_action_guide_me_step_by_step"
  ),
  UNDERSTAND_MISTAKE(
    label = "Help me understand my mistake",
    promptText = "Help me understand my mistake",
    icon = Icons.Default.Psychology,
    testTag = "quick_action_help_me_understand_my_mistake"
  ),
  EXPLAIN_EXAMPLE(
    label = "Explain with an example",
    promptText = "Explain with an example",
    icon = Icons.Default.AutoAwesome,
    testTag = "quick_action_explain_with_an_example"
  ),
  MAKE_EASIER(
    label = "Make it easier",
    promptText = "Make it easier",
    icon = Icons.AutoMirrored.Filled.HelpOutline,
    testTag = "quick_action_make_it_easier"
  ),
  CHALLENGE_ME(
    label = "Challenge me",
    promptText = "Challenge me",
    icon = Icons.Default.Bolt,
    testTag = "quick_action_challenge_me"
  ),
  GIVE_ANSWER(
    label = "Give me the answer",
    promptText = "Give me the answer",
    icon = Icons.Default.CheckCircle,
    testTag = "quick_action_give_me_the_answer"
  )
}
