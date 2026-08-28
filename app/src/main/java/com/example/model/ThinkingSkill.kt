package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*

/**
 * The four core thinking skill categories tracked in Socratic.
 */
enum class ThinkingSkillType(
  val title: String,
  val shortDescription: String,
  val icon: ImageVector,
  val containerColor: Color,
  val textColor: Color
) {
  LOGICAL_REASONING(
    title = "Logical Reasoning",
    shortDescription = "Deductive steps, cause-and-effect chains, and structural consistency",
    icon = Icons.Default.AccountTree,
    containerColor = PastelMathBg,
    textColor = PastelMathText
  ),
  CRITICAL_THINKING(
    title = "Critical Thinking",
    shortDescription = "Questioning premises, evaluating counter-evidence, and inquiry depth",
    icon = Icons.Default.Psychology,
    containerColor = PastelBiologyBg,
    textColor = PastelBiologyText
  ),
  PROBLEM_SOLVING(
    title = "Problem-Solving",
    shortDescription = "Decomposing complex questions into sequential, manageable sub-steps",
    icon = Icons.Default.Calculate,
    containerColor = PastelCSBg,
    textColor = PastelCSText
  ),
  CONCEPTUAL_UNDERSTANDING(
    title = "Conceptual Understanding",
    shortDescription = "First-principles mental models and linking abstract ideas to physical intuition",
    icon = Icons.Default.Lightbulb,
    containerColor = PastelProgrammingBg,
    textColor = PastelProgrammingText
  )
}

/**
 * Learning performance indicator for an individual thinking skill.
 * NOTE: These are estimated formative learning indicators based on in-app activities,
 * not standardized psychological or intelligence measurements.
 */
data class ThinkingSkillIndicator(
  val type: ThinkingSkillType,
  val scorePercent: Int, // 0 to 100
  val levelBadge: String, // e.g. "Developing", "Advancing", "Proficient", "Mastery"
  val trendLabel: String, // e.g. "Trending upward • +8% recently"
  val primaryFeedback: String, // High-level personalized feedback
  val actionableSuggestion: String, // Concrete next step
  val evidenceBasis: String, // Transparent description of in-app activity source
  val recommendedSubject: Subject,
  val practicePrompt: String
)

/**
 * Complete Thinking Skills profile containing all 4 core indicators,
 * summary metadata, and formative estimation notice.
 */
data class ThinkingSkillsProfile(
  val indicators: List<ThinkingSkillIndicator>,
  val overallIndex: Int,
  val disclaimerNotice: String = "These are estimated learning indicators based on your activity, dialogue answers, and quiz performance within the Socratic application. They are designed for formative study guidance and do not represent psychological or intelligence measurements.",
  val totalInquiriesAnalyzed: Int,
  val totalQuizQuestionsAnalyzed: Int
)
