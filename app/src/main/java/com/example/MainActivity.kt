package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.tutor.SocraticTutorRepository
import com.example.ui.components.SocraticBottomNav
import com.example.ui.components.SocraticDrawerContent
import com.example.ui.components.SocraticTopBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        SocraticApp()
      }
    }
  }
}

@Composable
fun SocraticApp() {
  val tutorRepository = remember { SocraticTutorRepository() }
  var currentNav by remember { mutableStateOf(NavItem.HOME) }
  var selectedSubject by remember { mutableStateOf(Subject.MATHEMATICS) }
  var selectedDifficulty by remember { mutableStateOf(Difficulty.BEGINNER) }
  var selectedLearningMode by remember { mutableStateOf(LearningMode.SOCRATIC) }
  var questionInput by remember { mutableStateOf("") }
  var activeTopic by remember { mutableStateOf("") }
  var isThinking by remember { mutableStateOf(false) }

  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  // Socratic Chat Session Messages State
  var messages by remember {
    mutableStateOf(
      listOf(
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "Welcome to Socratic! I'm here to guide your reasoning rather than simply giving answers. What would you like to explore today?",
          guidedQuestion = "Think about a concept that puzzles you. Where would you like to start?",
          suggestedAnswers = listOf(
            "Why does ice float on water?",
            "What is a Python function?",
            "Negative times negative intuition"
          )
        )
      )
    )
  }

  // Function to initialize a new learning inquiry
  fun startSocraticSession(
    subject: Subject,
    difficulty: Difficulty,
    topic: String,
    learningMode: LearningMode = selectedLearningMode
  ) {
    selectedSubject = subject
    selectedDifficulty = difficulty
    selectedLearningMode = learningMode
    val cleanTopic = topic.ifBlank { subject.sampleQuestions.first() }
    activeTopic = cleanTopic
    currentNav = NavItem.AI_TUTOR

    coroutineScope.launch {
      isThinking = true
      val initialInquiry = tutorRepository.startSession(subject, difficulty, cleanTopic, learningMode)
      messages = listOf(initialInquiry)
      isThinking = false
    }
  }

  // Function to respond to student input with progressive responses
  fun handleStudentMessage(userText: String) {
    val userMsg = ChatMessage(
      sender = MessageSender.STUDENT,
      text = userText
    )

    val updatedHistory = messages + userMsg
    messages = updatedHistory

    coroutineScope.launch {
      isThinking = true
      val nextTutorMsg = tutorRepository.sendStudentMessage(
        subject = selectedSubject,
        difficulty = selectedDifficulty,
        topic = activeTopic.ifBlank { selectedSubject.sampleQuestions.first() },
        history = updatedHistory,
        studentInput = userText,
        learningMode = selectedLearningMode
      )
      messages = updatedHistory + nextTutorMsg
      isThinking = false
    }
  }

  fun resetSession() {
    coroutineScope.launch {
      isThinking = true
      val initial = tutorRepository.startSession(
        selectedSubject,
        selectedDifficulty,
        activeTopic.ifBlank { selectedSubject.sampleQuestions.first() },
        selectedLearningMode
      )
      messages = listOf(initial)
      isThinking = false
      snackbarHostState.showSnackbar("Inquiry dialogue restarted.")
    }
  }

  fun switchLearningMode(newMode: LearningMode) {
    if (selectedLearningMode == newMode) return
    selectedLearningMode = newMode
    coroutineScope.launch {
      isThinking = true
      val adaptedInitial = tutorRepository.startSession(
        selectedSubject,
        selectedDifficulty,
        activeTopic.ifBlank { selectedSubject.sampleQuestions.first() },
        newMode
      )
      messages = listOf(adaptedInitial)
      isThinking = false
      snackbarHostState.showSnackbar("Switched mode to ${newMode.displayName}")
    }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      SocraticDrawerContent(
        currentNav = currentNav,
        onNavigate = { navItem ->
          currentNav = navItem
          coroutineScope.launch { drawerState.close() }
        },
        onSelectHistoryTopic = { topic, subject ->
          coroutineScope.launch { drawerState.close() }
          startSocraticSession(subject, selectedDifficulty, topic, selectedLearningMode)
        }
      )
    },
    modifier = Modifier.fillMaxSize()
  ) {
    Scaffold(
      topBar = {
        SocraticTopBar(
          currentNav = currentNav,
          selectedSubject = if (currentNav == NavItem.AI_TUTOR) selectedSubject else null,
          onOpenDrawer = {
            coroutineScope.launch {
              if (drawerState.isClosed) drawerState.open() else drawerState.close()
            }
          }
        )
      },
      bottomBar = {
        SocraticBottomNav(
          currentNav = currentNav,
          onNavigate = { navItem ->
            currentNav = navItem
          }
        )
      },
      snackbarHost = {
        SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.testTag("app_snackbar_host")
        )
      },
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .testTag("socratic_main_scaffold")
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.TopCenter
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 860.dp)
        ) {
          AnimatedContent(
            targetState = currentNav,
            transitionSpec = {
              fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "screen_navigation_transition"
          ) { targetNav ->
            when (targetNav) {
              NavItem.HOME -> {
                HomeScreen(
                  selectedSubject = selectedSubject,
                  onSubjectSelected = { selectedSubject = it },
                  selectedDifficulty = selectedDifficulty,
                  onDifficultySelected = { selectedDifficulty = it },
                  selectedLearningMode = selectedLearningMode,
                  onLearningModeSelected = { selectedLearningMode = it },
                  questionInput = questionInput,
                  onQuestionInputChange = { questionInput = it },
                  onStartLearning = {
                    val topicToStart = questionInput.ifBlank { selectedSubject.sampleQuestions.first() }
                    startSocraticSession(selectedSubject, selectedDifficulty, topicToStart, selectedLearningMode)
                  }
                )
              }

              NavItem.AI_TUTOR -> {
                AITutorScreen(
                  subject = selectedSubject,
                  difficulty = selectedDifficulty,
                  activeTopic = activeTopic.ifBlank { selectedSubject.sampleQuestions.first() },
                  learningMode = selectedLearningMode,
                  onLearningModeChange = { switchLearningMode(it) },
                  messages = messages,
                  isThinking = isThinking,
                  onSendMessage = { studentInput ->
                    handleStudentMessage(studentInput)
                  },
                  onResetSession = { resetSession() },
                  onShowSnackbar = { text ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(text) }
                  }
                )
              }

              NavItem.SUBJECTS -> {
                SubjectsScreen(
                  selectedSubject = selectedSubject,
                  onSelectSubjectAndExplore = { subject, prompt ->
                    selectedSubject = subject
                    questionInput = prompt
                    startSocraticSession(subject, selectedDifficulty, prompt, selectedLearningMode)
                  }
                )
              }

              NavItem.QUIZ -> {
                QuizScreen(
                  activeSubject = selectedSubject,
                  activeDifficulty = selectedDifficulty,
                  activeTopic = activeTopic.ifBlank { questionInput },
                  conversationHistory = messages,
                  onExploreInTutor = { subject, question ->
                    selectedSubject = subject
                    questionInput = question
                    startSocraticSession(subject, selectedDifficulty, question, selectedLearningMode)
                  }
                )
              }

              NavItem.PROGRESS -> {
                ProgressScreen(
                  conversationHistory = messages,
                  onExploreSkillInTutor = { subject, prompt ->
                    selectedSubject = subject
                    questionInput = prompt
                    startSocraticSession(subject, selectedDifficulty, prompt, selectedLearningMode)
                  },
                  onTakeSkillQuiz = { subject, prompt ->
                    selectedSubject = subject
                    questionInput = prompt
                    activeTopic = prompt
                    currentNav = NavItem.QUIZ
                  }
                )
              }

              NavItem.SETTINGS -> {
                SettingsScreen(
                  onShowSnackbar = { text ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(text) }
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}

