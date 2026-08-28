package com.example.model

enum class QuestionType(val displayName: String, val iconName: String) {
  MULTIPLE_CHOICE("Multiple Choice", "checklist"),
  SHORT_ANSWER("Short Answer", "short_text"),
  PROBLEM_SOLVING("Problem Solving", "calculate"),
  CONCEPTUAL("Conceptual", "psychology")
}

data class QuizQuestion(
  val id: String,
  val type: QuestionType,
  val prompt: String,
  val context: String = "",
  val options: List<String> = emptyList(),
  val correctOptionIndex: Int? = null,
  val correctAnswerText: String,
  val acceptableKeywords: List<String> = emptyList(),
  val conceptTag: String,
  val explanation: String,
  val improvementTip: String = ""
)

data class Quiz(
  val id: String,
  val subject: Subject,
  val topic: String,
  val difficulty: Difficulty,
  val basedOnConversation: Boolean = false,
  val questions: List<QuizQuestion>
)

data class QuestionResult(
  val question: QuizQuestion,
  val userResponse: String,
  val isCorrect: Boolean,
  val feedbackNote: String = ""
)

data class ConceptImprovementArea(
  val conceptName: String,
  val subject: Subject,
  val description: String,
  val remedialPrompt: String
)

data class QuizEvaluation(
  val quiz: Quiz,
  val results: List<QuestionResult>,
  val score: Int,
  val totalQuestions: Int,
  val percentage: Int,
  val conceptsToImprove: List<ConceptImprovementArea>
)
