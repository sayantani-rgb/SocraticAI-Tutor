package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Science
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*

enum class Subject(
  val displayName: String,
  val icon: ImageVector,
  val pastelBg: Color,
  val accentColor: Color,
  val description: String,
  val sampleQuestions: List<String>
) {
  MATHEMATICS(
    displayName = "Mathematics",
    icon = Icons.Default.Calculate,
    pastelBg = PastelMathBg,
    accentColor = PastelMathText,
    description = "Logic, algebra, calculus, geometry, and numerical reasoning",
    sampleQuestions = listOf(
      "Why does a negative times a negative equal a positive?",
      "How does the Pythagorean theorem work intuitively?",
      "What is the intuitive meaning of a derivative in calculus?"
    )
  ),
  PHYSICS(
    displayName = "Physics",
    icon = Icons.Default.Science,
    pastelBg = PastelPhysicsBg,
    accentColor = PastelPhysicsText,
    description = "Mechanics, thermodynamics, electromagnetism, and the universe",
    sampleQuestions = listOf(
      "Why does ice float on water when most solids sink?",
      "How does Newton's third law apply when pushing against a wall?",
      "Why is the sky blue during the day and red at sunset?"
    )
  ),
  CHEMISTRY(
    displayName = "Chemistry",
    icon = Icons.Default.Biotech,
    pastelBg = PastelChemistryBg,
    accentColor = PastelChemistryText,
    description = "Atoms, molecules, chemical bonding, and reactions",
    sampleQuestions = listOf(
      "Why do atoms form covalent bonds rather than ionic bonds?",
      "How does pH change when an acid is diluted with water?",
      "Why does salt lower the freezing point of water?"
    )
  ),
  BIOLOGY(
    displayName = "Biology",
    icon = Icons.Default.AutoStories,
    pastelBg = PastelBiologyBg,
    accentColor = PastelBiologyText,
    description = "Cellular life, genetics, evolution, and ecosystems",
    sampleQuestions = listOf(
      "How do enzymes lower activation energy in biochemical reactions?",
      "Why is cellular respiration considered the reverse of photosynthesis?",
      "How does natural selection drive evolutionary adaptation?"
    )
  ),
  COMPUTER_SCIENCE(
    displayName = "Computer Science",
    icon = Icons.Default.Memory,
    pastelBg = PastelCSBg,
    accentColor = PastelCSText,
    description = "Algorithms, data structures, complexity, and systems",
    sampleQuestions = listOf(
      "Why is binary search O(log n) while linear search is O(n)?",
      "How does recursion use the call stack under the hood?",
      "What makes a hash table lookup average O(1) time complexity?"
    )
  ),
  PROGRAMMING(
    displayName = "Programming",
    icon = Icons.Default.Code,
    pastelBg = PastelProgrammingBg,
    accentColor = PastelProgrammingText,
    description = "Kotlin, Python, logic flow, debugging, and software design",
    sampleQuestions = listOf(
      "What is a Python function?",
      "Why should mutable state be encapsulated in modern architecture?",
      "What is the difference between concurrency and parallelism?"
    )
  ),
  ENGLISH(
    displayName = "English",
    icon = Icons.Default.Language,
    pastelBg = PastelEnglishBg,
    accentColor = PastelEnglishText,
    description = "Literature, rhetoric, grammar, and critical analysis",
    sampleQuestions = listOf(
      "How does Shakespeare use dramatic irony to build suspense?",
      "What makes an argument persuasive according to Aristotle's ethos, pathos, and logos?",
      "How does tone differ from mood in narrative prose?"
    )
  ),
  HISTORY(
    displayName = "History",
    icon = Icons.Default.HistoryEdu,
    pastelBg = PastelHistoryBg,
    accentColor = PastelHistoryText,
    description = "World civilizations, revolutions, diplomacy, and societal shifts",
    sampleQuestions = listOf(
      "What were the underlying economic causes of the Industrial Revolution?",
      "How did the printing press transform medieval European society?",
      "Why did the League of Nations fail to prevent global conflict?"
    )
  ),
  OTHER(
    displayName = "Other",
    icon = Icons.Default.MoreHoriz,
    pastelBg = PastelOtherBg,
    accentColor = PastelOtherText,
    description = "Philosophy, economics, art theory, and interdisciplinary topics",
    sampleQuestions = listOf(
      "What is the difference between deductive and inductive reasoning?",
      "How does opportunity cost influence everyday decision-making?",
      "What defines the Socratic method of dialogue and inquiry?"
    )
  )
}
