package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.quiz.QuizRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

private enum class QuizScreenState {
  CONFIGURING,
  GENERATING,
  TAKING,
  RESULTS
}

private enum class ResultFilter {
  ALL,
  CORRECT,
  INCORRECT
}

@Composable
fun QuizScreen(
  activeSubject: Subject = Subject.MATHEMATICS,
  activeDifficulty: Difficulty = Difficulty.BEGINNER,
  activeTopic: String = "",
  conversationHistory: List<ChatMessage> = emptyList(),
  onExploreInTutor: (Subject, String) -> Unit,
  modifier: Modifier = Modifier
) {
  val quizRepository = remember { QuizRepository() }
  val coroutineScope = rememberCoroutineScope()

  // Screen state
  var screenState by remember { mutableStateOf(QuizScreenState.CONFIGURING) }

  // Generation parameters
  var selectedSubject by remember { mutableStateOf(activeSubject) }
  var selectedDifficulty by remember { mutableStateOf(activeDifficulty) }
  var topicInput by remember { mutableStateOf(if (activeTopic.isNotBlank()) activeTopic else activeSubject.sampleQuestions.first()) }
  var includeConversation by remember { mutableStateOf(conversationHistory.size > 1) }

  // Active Quiz State
  var currentQuiz by remember { mutableStateOf<Quiz?>(null) }
  var currentQuestionIndex by remember { mutableStateOf(0) }
  val userAnswers = remember { mutableStateMapOf<String, String>() }
  var quizEvaluation by remember { mutableStateOf<QuizEvaluation?>(null) }
  var showHint by remember { mutableStateOf(false) }
  var resultFilter by remember { mutableStateOf(ResultFilter.ALL) }

  // Synchronize initial parameters when props change and still in CONFIGURING
  LaunchedEffect(activeSubject, activeTopic, activeDifficulty) {
    if (screenState == QuizScreenState.CONFIGURING && currentQuiz == null) {
      selectedSubject = activeSubject
      selectedDifficulty = activeDifficulty
      if (activeTopic.isNotBlank()) {
        topicInput = activeTopic
      }
      includeConversation = conversationHistory.size > 1
    }
  }

  fun startQuizGeneration() {
    coroutineScope.launch {
      screenState = QuizScreenState.GENERATING
      try {
        val generated = quizRepository.generateQuiz(
          subject = selectedSubject,
          topic = topicInput.ifBlank { selectedSubject.sampleQuestions.first() },
          difficulty = selectedDifficulty,
          history = if (includeConversation) conversationHistory else null,
          includeConversation = includeConversation
        )
        currentQuiz = generated
        userAnswers.clear()
        currentQuestionIndex = 0
        showHint = false
        screenState = QuizScreenState.TAKING
      } catch (e: Exception) {
        screenState = QuizScreenState.CONFIGURING
      }
    }
  }

  fun submitQuiz() {
    val quiz = currentQuiz ?: return
    val evaluation = quizRepository.evaluateQuiz(quiz, userAnswers.toMap())
    quizEvaluation = evaluation
    screenState = QuizScreenState.RESULTS
  }

  fun retakeQuiz() {
    userAnswers.clear()
    currentQuestionIndex = 0
    showHint = false
    screenState = QuizScreenState.TAKING
  }

  fun resetToConfig() {
    userAnswers.clear()
    currentQuiz = null
    quizEvaluation = null
    currentQuestionIndex = 0
    showHint = false
    screenState = QuizScreenState.CONFIGURING
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("quiz_screen_root")
  ) {
    when (screenState) {
      QuizScreenState.CONFIGURING -> {
        QuizConfigView(
          selectedSubject = selectedSubject,
          onSelectSubject = {
            selectedSubject = it
            topicInput = it.sampleQuestions.first()
          },
          selectedDifficulty = selectedDifficulty,
          onSelectDifficulty = { selectedDifficulty = it },
          topicInput = topicInput,
          onTopicInputChange = { topicInput = it },
          conversationHistory = conversationHistory,
          includeConversation = includeConversation,
          onToggleIncludeConversation = { includeConversation = it },
          onGenerateQuiz = { startQuizGeneration() }
        )
      }

      QuizScreenState.GENERATING -> {
        QuizGeneratingView(
          subject = selectedSubject,
          topic = topicInput.ifBlank { selectedSubject.sampleQuestions.first() },
          difficulty = selectedDifficulty
        )
      }

      QuizScreenState.TAKING -> {
        val quiz = currentQuiz
        if (quiz != null && quiz.questions.isNotEmpty()) {
          QuizTakingView(
            quiz = quiz,
            currentQuestionIndex = currentQuestionIndex,
            onQuestionIndexChange = {
              currentQuestionIndex = it
              showHint = false
            },
            userAnswers = userAnswers,
            onAnswerChange = { qId, answer ->
              userAnswers[qId] = answer
            },
            showHint = showHint,
            onToggleHint = { showHint = !showHint },
            onSubmit = { submitQuiz() },
            onCancelQuiz = { resetToConfig() }
          )
        } else {
          // Fallback if quiz is null
          resetToConfig()
        }
      }

      QuizScreenState.RESULTS -> {
        val evaluation = quizEvaluation
        if (evaluation != null) {
          QuizResultsView(
            evaluation = evaluation,
            resultFilter = resultFilter,
            onFilterChange = { resultFilter = it },
            onRetakeQuiz = { retakeQuiz() },
            onNewQuiz = { resetToConfig() },
            onExploreInTutor = onExploreInTutor
          )
        } else {
          resetToConfig()
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 1. CONFIGURATION VIEW
// -------------------------------------------------------------
@Composable
private fun QuizConfigView(
  selectedSubject: Subject,
  onSelectSubject: (Subject) -> Unit,
  selectedDifficulty: Difficulty,
  onSelectDifficulty: (Difficulty) -> Unit,
  topicInput: String,
  onTopicInputChange: (String) -> Unit,
  conversationHistory: List<ChatMessage>,
  includeConversation: Boolean,
  onToggleIncludeConversation: (Boolean) -> Unit,
  onGenerateQuiz: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("quiz_config_container"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header Banner
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Quiz,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "AI Diagnostic Quiz",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Generate targeted multi-format quizzes with MCQs, short answers, problem-solving, and conceptual questions.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // 1. Subject Selection
    item {
      Text(
        text = "1. CHOOSE SUBJECT",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp),
        color = EditorialMuted
      )
      Spacer(modifier = Modifier.height(6.dp))

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(Subject.values()) { subject ->
          val isSelected = subject == selectedSubject
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) subject.pastelBg else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
              if (isSelected) 2.dp else 1.dp,
              if (isSelected) subject.accentColor else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .clickable { onSelectSubject(subject) }
              .testTag("quiz_subject_chip_${subject.name.lowercase()}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = subject.icon,
                contentDescription = null,
                tint = if (isSelected) subject.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = subject.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                color = if (isSelected) subject.accentColor else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }

    // 2. Topic Input & Suggestions
    item {
      Text(
        text = "2. TOPIC OR QUESTION TO TEST",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp),
        color = EditorialMuted
      )
      Spacer(modifier = Modifier.height(6.dp))

      OutlinedTextField(
        value = topicInput,
        onValueChange = onTopicInputChange,
        placeholder = { Text("e.g. Why does ice float on water?, Python functions...") },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        leadingIcon = {
          Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingIcon = {
          if (topicInput.isNotBlank()) {
            IconButton(onClick = { onTopicInputChange("") }) {
              Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("quiz_topic_input")
      )

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Suggested ${selectedSubject.displayName} Topics:",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(4.dp))

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(selectedSubject.sampleQuestions) { sample ->
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .clickable { onTopicInputChange(sample) }
          ) {
            Text(
              text = sample,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }
      }
    }

    // 3. Difficulty Level
    item {
      Text(
        text = "3. DIFFICULTY LEVEL",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp),
        color = EditorialMuted
      )
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Difficulty.values().forEach { diff ->
          val isSelected = diff == selectedDifficulty
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
              if (isSelected) 2.dp else 1.dp,
              if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .clickable { onSelectDifficulty(diff) }
              .testTag("quiz_diff_${diff.name.lowercase()}")
          ) {
            Column(
              modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = diff.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = diff.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = diff.levelBadge,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = EditorialMuted
              )
            }
          }
        }
      }
    }

    // 4. Previous Learning Conversation Context Toggle
    item {
      val hasConversation = conversationHistory.size > 1
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (hasConversation && includeConversation) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Psychology,
              contentDescription = null,
              tint = if (hasConversation) MaterialTheme.colorScheme.primary else EditorialMuted,
              modifier = Modifier.size(24.dp)
            )
            Column {
              Text(
                text = "Based on Previous Conversation",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (hasConversation) {
                  "Adapts questions to your recent chat dialog and concepts."
                } else {
                  "No active chat dialog found; standard topic knowledge will be used."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = includeConversation && hasConversation,
            onCheckedChange = { onToggleIncludeConversation(it) },
            enabled = hasConversation,
            modifier = Modifier.testTag("quiz_include_conversation_toggle")
          )
        }
      }
    }

    // Question Types Preview
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "INCLUDED QUESTION TYPES (4 TOTAL)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 10.sp),
            color = EditorialMuted
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            QuestionType.values().forEach { qType ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.weight(1f)
              ) {
                Column(
                  modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  val icon = when (qType) {
                    QuestionType.MULTIPLE_CHOICE -> Icons.Default.Checklist
                    QuestionType.SHORT_ANSWER -> Icons.AutoMirrored.Filled.ShortText
                    QuestionType.PROBLEM_SOLVING -> Icons.Default.Calculate
                    QuestionType.CONCEPTUAL -> Icons.Default.Psychology
                  }
                  Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                  Text(
                    text = qType.displayName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                  )
                }
              }
            }
          }
        }
      }
    }

    // Generate Button
    item {
      Button(
        onClick = onGenerateQuiz,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("quiz_generate_button")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
          Text(
            text = "Generate Quiz with AI",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 2. GENERATING VIEW (LOADING)
// -------------------------------------------------------------
@Composable
private fun QuizGeneratingView(
  subject: Subject,
  topic: String,
  difficulty: Difficulty
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp)
      .testTag("quiz_generating_container"),
    contentAlignment = Alignment.Center
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(52.dp),
          color = MaterialTheme.colorScheme.primary,
          strokeWidth = 4.dp
        )

        Text(
          text = "Synthesizing AI Diagnostic Quiz...",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = subject.pastelBg
        ) {
          Text(
            text = "${subject.displayName} • ${difficulty.displayName}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = subject.accentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }

        Text(
          text = "\"$topic\"",
          style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        Text(
          text = "Formulating 4 specialized questions (MCQ, Short Answer, Problem Solving, Conceptual) with rubric keys...",
          style = MaterialTheme.typography.bodySmall,
          color = EditorialMuted,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

// -------------------------------------------------------------
// 3. TAKING VIEW
// -------------------------------------------------------------
@Composable
private fun QuizTakingView(
  quiz: Quiz,
  currentQuestionIndex: Int,
  onQuestionIndexChange: (Int) -> Unit,
  userAnswers: Map<String, String>,
  onAnswerChange: (String, String) -> Unit,
  showHint: Boolean,
  onToggleHint: () -> Unit,
  onSubmit: () -> Unit,
  onCancelQuiz: () -> Unit
) {
  val question = quiz.questions[currentQuestionIndex]
  val totalQuestions = quiz.questions.size
  val answeredCount = quiz.questions.count { userAnswers[it.id]?.isNotBlank() == true }
  val currentAnswer = userAnswers[question.id] ?: ""

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("quiz_taking_container"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Top Bar Metadata & Progress
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = quiz.subject.pastelBg
              ) {
                Text(
                  text = quiz.subject.displayName,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = quiz.subject.accentColor,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
              ) {
                Text(
                  text = quiz.difficulty.displayName,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            TextButton(
              onClick = onCancelQuiz,
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                "Exit Quiz",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error)
              )
            }
          }

          // Progress Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Question ${currentQuestionIndex + 1} of $totalQuestions",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "$answeredCount/$totalQuestions Answered",
              style = MaterialTheme.typography.labelSmall,
              color = EditorialMuted
            )
          }

          LinearProgressIndicator(
            progress = { (currentQuestionIndex + 1).toFloat() / totalQuestions.toFloat() },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )
        }
      }
    }

    // Question Card
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Question Type Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                val icon = when (question.type) {
                  QuestionType.MULTIPLE_CHOICE -> Icons.Default.Checklist
                  QuestionType.SHORT_ANSWER -> Icons.AutoMirrored.Filled.ShortText
                  QuestionType.PROBLEM_SOLVING -> Icons.Default.Calculate
                  QuestionType.CONCEPTUAL -> Icons.Default.Psychology
                }
                Icon(
                  imageVector = icon,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = question.type.displayName.uppercase(),
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
            }

            if (question.context.isNotBlank()) {
              IconButton(onClick = onToggleHint) {
                Icon(
                  imageVector = if (showHint) Icons.Default.Lightbulb else Icons.AutoMirrored.Filled.HelpOutline,
                  contentDescription = "Toggle Context Hint",
                  tint = if (showHint) MaterialTheme.colorScheme.primary else EditorialMuted
                )
              }
            }
          }

          // Question Prompt
          Text(
            text = question.prompt,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
            color = MaterialTheme.colorScheme.onSurface
          )

          // Optional Context Card
          if (showHint && question.context.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Lightbulb,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = question.context,
                  style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          // Interactive Question Answering UI based on Type
          when (question.type) {
            QuestionType.MULTIPLE_CHOICE -> {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.options.forEachIndexed { index, option ->
                  val isSelected = currentAnswer == index.toString() || currentAnswer == option

                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                      if (isSelected) 2.dp else 1.dp,
                      if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(14.dp))
                      .clickable {
                        onAnswerChange(question.id, index.toString())
                      }
                      .testTag("quiz_mcq_option_$index")
                  ) {
                    Row(
                      modifier = Modifier.padding(14.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(28.dp)
                          .clip(CircleShape)
                          .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          text = ('A' + index).toString(),
                          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                          color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }

                      Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                      )
                    }
                  }
                }
              }
            }

            QuestionType.SHORT_ANSWER -> {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                  value = currentAnswer,
                  onValueChange = { onAnswerChange(question.id, it) },
                  placeholder = { Text("Enter your concise answer (term, formula, or phrase)...") },
                  shape = RoundedCornerShape(14.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                  ),
                  singleLine = true,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_short_answer_input")
                )
                Text(
                  text = "Tip: Write the exact term or concise definition.",
                  style = MaterialTheme.typography.labelSmall,
                  color = EditorialMuted
                )
              }
            }

            QuestionType.PROBLEM_SOLVING -> {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                  value = currentAnswer,
                  onValueChange = { onAnswerChange(question.id, it) },
                  placeholder = { Text("Write your calculated numerical result or code derivation...") },
                  shape = RoundedCornerShape(14.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                  ),
                  minLines = 3,
                  maxLines = 6,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_problem_solving_input")
                )
                Text(
                  text = "Tip: Include your final numerical result or key deduction steps.",
                  style = MaterialTheme.typography.labelSmall,
                  color = EditorialMuted
                )
              }
            }

            QuestionType.CONCEPTUAL -> {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                  value = currentAnswer,
                  onValueChange = { onAnswerChange(question.id, it) },
                  placeholder = { Text("Explain the fundamental intuition and underlying mechanism in your own words...") },
                  shape = RoundedCornerShape(14.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                  ),
                  minLines = 4,
                  maxLines = 8,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_conceptual_input")
                )
                Text(
                  text = "Tip: Focus on first principles—why the system behaves this way.",
                  style = MaterialTheme.typography.labelSmall,
                  color = EditorialMuted
                )
              }
            }
          }
        }
      }
    }

    // Question Navigation & Jumper
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = { onQuestionIndexChange(currentQuestionIndex - 1) },
          enabled = currentQuestionIndex > 0,
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Prev")
        }

        // Dot indicators
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          quiz.questions.forEachIndexed { idx, q ->
            val isCurrent = idx == currentQuestionIndex
            val isAnswered = userAnswers[q.id]?.isNotBlank() == true

            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                  when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isAnswered -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                  }
                )
                .clickable { onQuestionIndexChange(idx) },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = (idx + 1).toString(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = when {
                  isCurrent -> MaterialTheme.colorScheme.onPrimary
                  isAnswered -> MaterialTheme.colorScheme.onPrimaryContainer
                  else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
              )
            }
          }
        }

        if (currentQuestionIndex < totalQuestions - 1) {
          Button(
            onClick = { onQuestionIndexChange(currentQuestionIndex + 1) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Text("Next")
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
          }
        } else {
          Button(
            onClick = onSubmit,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EditorialSuccess),
            modifier = Modifier.testTag("quiz_submit_button")
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Submit")
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 4. RESULTS VIEW
// -------------------------------------------------------------
@Composable
private fun QuizResultsView(
  evaluation: QuizEvaluation,
  resultFilter: ResultFilter,
  onFilterChange: (ResultFilter) -> Unit,
  onRetakeQuiz: () -> Unit,
  onNewQuiz: () -> Unit,
  onExploreInTutor: (Subject, String) -> Unit
) {
  val isHighMastery = evaluation.percentage >= 75

  val filteredResults = remember(evaluation, resultFilter) {
    when (resultFilter) {
      ResultFilter.ALL -> evaluation.results
      ResultFilter.CORRECT -> evaluation.results.filter { it.isCorrect }
      ResultFilter.INCORRECT -> evaluation.results.filter { !it.isCorrect }
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("quiz_results_container"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Hero Score Card
    item {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isHighMastery) PastelChemistryBg else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
          1.dp,
          if (isHighMastery) PastelChemistryText else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = if (isHighMastery) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.TrendingUp,
              contentDescription = null,
              tint = if (isHighMastery) PastelChemistryText else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Text(
              text = if (isHighMastery) "Mastery Demonstrated!" else "Diagnostic Evaluation Complete",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = if (isHighMastery) PastelChemistryText else MaterialTheme.colorScheme.onSurface
            )
          }

          Text(
            text = "${evaluation.percentage}%",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = if (isHighMastery) PastelChemistryText else MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("quiz_result_percentage")
          )

          Text(
            text = "${evaluation.score} of ${evaluation.totalQuestions} Questions Correct",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
          ) {
            Text(
              text = "${evaluation.quiz.subject.displayName} • ${evaluation.quiz.difficulty.displayName} • ${evaluation.quiz.topic}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              textAlign = TextAlign.Center
            )
          }
        }
      }
    }

    // 2. Concepts That Need Improvement (MANDATORY REQUIREMENT)
    if (evaluation.conceptsToImprove.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("concepts_to_improve_container")
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "Concepts That Need Improvement",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
              )
            }

            Text(
              text = "The AI identified the following foundational principles where further intuitive exploration will solidify your reasoning:",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )

            evaluation.conceptsToImprove.forEach { concept ->
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = concept.conceptName,
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = concept.subject.pastelBg
                    ) {
                      Text(
                        text = concept.subject.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = concept.subject.accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }

                  Text(
                    text = concept.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  Button(
                    onClick = {
                      onExploreInTutor(concept.subject, concept.remedialPrompt)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                      .align(Alignment.End)
                      .testTag("explore_concept_in_tutor_${concept.conceptName.take(10).lowercase().replace(" ", "_")}")
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                      Text("Explore in AI Tutor", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                  }
                }
              }
            }
          }
        }
      }
    } else {
      // 100% Score Congratulations
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = PastelChemistryBg),
          border = BorderStroke(1.dp, PastelChemistryText.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PastelChemistryText, modifier = Modifier.size(24.dp))
            Column {
              Text(
                text = "Flawless First-Principles Reasoning!",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = PastelChemistryText
              )
              Text(
                text = "All question formats solved correctly. You're ready to try the next difficulty level or a new subject!",
                style = MaterialTheme.typography.bodySmall,
                color = PastelChemistryText
              )
            }
          }
        }
      }
    }

    // 3. Question Evaluation Breakdown with Filter Tabs
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "DETAILED QUESTION REVIEW",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
          color = EditorialMuted
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          ResultFilter.values().forEach { filter ->
            val isSelected = filter == resultFilter
            val count = when (filter) {
              ResultFilter.ALL -> evaluation.results.size
              ResultFilter.CORRECT -> evaluation.score
              ResultFilter.INCORRECT -> evaluation.totalQuestions - evaluation.score
            }

            Surface(
              shape = RoundedCornerShape(50),
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
              border = BorderStroke(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
              ),
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onFilterChange(filter) }
            ) {
              Text(
                text = "${filter.name.lowercase().replaceFirstChar { it.uppercase() }} ($count)",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }

    // Question Cards
    itemsIndexed(filteredResults) { index, result ->
      val q = result.question
      val isCorrect = result.isCorrect

      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
          1.dp,
          if (isCorrect) PastelChemistryText.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("result_question_card_${q.id}")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Status Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isCorrect) PastelChemistryBg else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                  contentDescription = null,
                  tint = if (isCorrect) PastelChemistryText else MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = if (isCorrect) "Correct" else "Incorrect",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (isCorrect) PastelChemistryText else MaterialTheme.colorScheme.error
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                text = q.type.displayName,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          // Question Prompt
          Text(
            text = q.prompt,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )

          // User Response Box
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isCorrect) PastelChemistryBg.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
            border = BorderStroke(
              1.dp,
              if (isCorrect) PastelChemistryText.copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
            )
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "Your Answer:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isCorrect) PastelChemistryText else MaterialTheme.colorScheme.error
              )

              val userAnsDisplay = if (q.type == QuestionType.MULTIPLE_CHOICE) {
                val idx = result.userResponse.toIntOrNull()
                if (idx != null && idx in q.options.indices) {
                  "(${('A' + idx)}) ${q.options[idx]}"
                } else if (result.userResponse.isNotBlank()) {
                  result.userResponse
                } else {
                  "(No answer selected)"
                }
              } else {
                result.userResponse.ifBlank { "(Left blank)" }
              }

              Text(
                text = userAnsDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          // Correct Answer / Solution Box
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "Expected / Canonical Answer:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )

              val correctAnsDisplay = if (q.type == QuestionType.MULTIPLE_CHOICE && q.correctOptionIndex != null) {
                "(${('A' + q.correctOptionIndex)}) ${q.options.getOrElse(q.correctOptionIndex) { q.correctAnswerText }}"
              } else {
                q.correctAnswerText
              }

              Text(
                text = correctAnsDisplay,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }

          // Detailed Socratic Explanation (MANDATORY REQUIREMENT)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Psychology,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "Conceptual Explanation & Intuition:",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Text(
                text = q.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Ask Tutor about this question
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(
              onClick = {
                onExploreInTutor(
                  evaluation.quiz.subject,
                  "In our quiz on '${evaluation.quiz.topic}', regarding the question: '${q.prompt}', can you guide my reasoning through why '${q.correctAnswerText}' is the correct principle?"
                )
              }
            ) {
              Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Ask AI Tutor about this", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
          }
        }
      }
    }

    // 4. Action Buttons at the Bottom
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onRetakeQuiz,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Retake Quiz", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }

        Button(
          onClick = onNewQuiz,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("quiz_new_quiz_button")
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New Quiz", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }
      }
    }
  }
}
