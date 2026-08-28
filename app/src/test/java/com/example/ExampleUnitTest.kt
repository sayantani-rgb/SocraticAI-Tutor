package com.example

import com.example.model.Difficulty
import com.example.model.LearningMode
import com.example.model.MessageSender
import com.example.model.QuestionType
import com.example.model.Subject
import com.example.model.ThinkingSkillType
import com.example.progress.ThinkingSkillsRepository
import com.example.quiz.QuizRepository
import com.example.tutor.SocraticTutorRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  private val repository = SocraticTutorRepository()
  private val quizRepository = QuizRepository()
  private val thinkingSkillsRepository = ThinkingSkillsRepository()

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testLearningModesInitialInquiry() = runBlocking {
    val socraticMsg = repository.startSession(
      Subject.COMPUTER_SCIENCE,
      Difficulty.BEGINNER,
      "What is a Python function?",
      LearningMode.SOCRATIC
    )
    assertEquals(MessageSender.TUTOR, socraticMsg.sender)
    assertNotNull(socraticMsg.guidedQuestion)

    val hintMsg = repository.startSession(
      Subject.COMPUTER_SCIENCE,
      Difficulty.BEGINNER,
      "What is a Python function?",
      LearningMode.HINT
    )
    assertEquals(MessageSender.TUTOR, hintMsg.sender)
    assertTrue(hintMsg.text.contains("Hint", ignoreCase = true) || hintMsg.text.contains("code", ignoreCase = true))

    val explainMsg = repository.startSession(
      Subject.COMPUTER_SCIENCE,
      Difficulty.BEGINNER,
      "What is a Python function?",
      LearningMode.EXPLAIN
    )
    assertEquals(MessageSender.TUTOR, explainMsg.sender)
    assertTrue(explainMsg.text.contains("function", ignoreCase = true) || explainMsg.text.contains("reusable", ignoreCase = true))

    val challengeMsg = repository.startSession(
      Subject.COMPUTER_SCIENCE,
      Difficulty.BEGINNER,
      "What is a Python function?",
      LearningMode.CHALLENGE
    )
    assertEquals(MessageSender.TUTOR, challengeMsg.sender)
    assertNotNull(challengeMsg.guidedQuestion)
  }

  @Test
  fun testQuickActionResponses() = runBlocking {
    val initialMsg = repository.startSession(
      Subject.COMPUTER_SCIENCE,
      Difficulty.BEGINNER,
      "What is a Python function?",
      LearningMode.SOCRATIC
    )
    val history = listOf(initialMsg)

    val quickActions = listOf(
      "Give me a hint",
      "Guide me step by step",
      "Help me understand my mistake",
      "Explain with an example",
      "Make it easier",
      "Challenge me",
      "Give me the answer"
    )

    for (action in quickActions) {
      val response = repository.sendStudentMessage(
        Subject.COMPUTER_SCIENCE,
        Difficulty.BEGINNER,
        "What is a Python function?",
        history,
        action,
        LearningMode.SOCRATIC
      )
      assertEquals(MessageSender.TUTOR, response.sender)
      assertNotNull(response.guidedQuestion)
      assertTrue(response.text.isNotBlank())
    }
  }

  @Test
  fun testGenerateQuizAllQuestionTypes() = runBlocking {
    val quiz = quizRepository.generateQuiz(
      subject = Subject.PHYSICS,
      topic = "Why does ice float on water?",
      difficulty = Difficulty.BEGINNER
    )

    assertEquals(Subject.PHYSICS, quiz.subject)
    assertEquals(4, quiz.questions.size)

    val types = quiz.questions.map { it.type }.toSet()
    assertTrue(types.contains(QuestionType.MULTIPLE_CHOICE))
    assertTrue(types.contains(QuestionType.SHORT_ANSWER))
    assertTrue(types.contains(QuestionType.PROBLEM_SOLVING))
    assertTrue(types.contains(QuestionType.CONCEPTUAL))

    // Check MCQ has valid options
    val mcq = quiz.questions.first { it.type == QuestionType.MULTIPLE_CHOICE }
    assertTrue(mcq.options.isNotEmpty())
    assertNotNull(mcq.correctOptionIndex)
    assertTrue(mcq.explanation.isNotBlank())
  }

  @Test
  fun testQuizEvaluationAndConceptsToImprove() = runBlocking {
    val quiz = quizRepository.generateQuiz(
      subject = Subject.COMPUTER_SCIENCE,
      topic = "What is a Python function?",
      difficulty = Difficulty.BEGINNER
    )

    // Provide answers: 1 correct (MCQ index 1), other incorrect/blank
    val mcq = quiz.questions.first { it.type == QuestionType.MULTIPLE_CHOICE }
    val userAnswers = mapOf(
      mcq.id to "1"
    )

    val evaluation = quizRepository.evaluateQuiz(quiz, userAnswers)
    assertEquals(1, evaluation.score)
    assertEquals(4, evaluation.totalQuestions)
    assertEquals(25, evaluation.percentage)

    // Should generate improvement areas for the remaining 3 questions
    assertEquals(3, evaluation.conceptsToImprove.size)
    for (concept in evaluation.conceptsToImprove) {
      assertTrue(concept.conceptName.isNotBlank())
      assertTrue(concept.remedialPrompt.contains("AI Tutor", ignoreCase = true) || concept.remedialPrompt.contains("help", ignoreCase = true))
    }
  }

  @Test
  fun testThinkingSkillsProfileGeneration() {
    val profile = thinkingSkillsRepository.calculateThinkingSkills(
      conversationHistory = emptyList(),
      totalInquiries = 14,
      activeSubject = Subject.MATHEMATICS
    )

    // Verify all 4 required indicators are generated
    assertEquals(4, profile.indicators.size)
    val skillTypes = profile.indicators.map { it.type }.toSet()
    assertTrue(skillTypes.contains(ThinkingSkillType.LOGICAL_REASONING))
    assertTrue(skillTypes.contains(ThinkingSkillType.CRITICAL_THINKING))
    assertTrue(skillTypes.contains(ThinkingSkillType.PROBLEM_SOLVING))
    assertTrue(skillTypes.contains(ThinkingSkillType.CONCEPTUAL_UNDERSTANDING))

    // Verify disclaimer explicitly states they are estimated learning indicators and not psychological or intelligence measurements
    assertTrue(profile.disclaimerNotice.contains("estimated learning indicators", ignoreCase = true))
    assertTrue(profile.disclaimerNotice.contains("psychological or intelligence", ignoreCase = true))

    // Verify useful feedback examples
    val problemSolving = profile.indicators.first { it.type == ThinkingSkillType.PROBLEM_SOLVING }
    assertTrue(problemSolving.primaryFeedback.contains("breaking complex problems into smaller steps", ignoreCase = true))
    assertTrue(problemSolving.scorePercent in 60..100)

    val conceptual = profile.indicators.first { it.type == ThinkingSkillType.CONCEPTUAL_UNDERSTANDING }
    assertTrue(conceptual.primaryFeedback.contains("practicing conceptual questions", ignoreCase = true))
    assertTrue(conceptual.scorePercent in 50..100)
    assertTrue(conceptual.actionableSuggestion.isNotBlank())
    assertTrue(conceptual.evidenceBasis.isNotBlank())
  }
}
