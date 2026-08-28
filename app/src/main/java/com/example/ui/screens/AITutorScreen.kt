package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.Difficulty
import com.example.model.LearningMode
import com.example.model.MessageSender
import com.example.model.QuickAction
import com.example.model.Subject
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AITutorScreen(
  subject: Subject,
  difficulty: Difficulty,
  activeTopic: String,
  learningMode: LearningMode,
  onLearningModeChange: (LearningMode) -> Unit,
  messages: List<ChatMessage>,
  isThinking: Boolean,
  onSendMessage: (String) -> Unit,
  onResetSession: () -> Unit,
  onShowSnackbar: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val clipboardManager = LocalClipboardManager.current

  LaunchedEffect(messages.size, isThinking) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1 + if (isThinking) 1 else 0)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("ai_tutor_screen_container")
  ) {
    // Topic & Mode Header Banner
    Surface(
      shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      shadowElevation = 1.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        // Top row: Subject, Difficulty, and Restart Session
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
              shape = RoundedCornerShape(50),
              color = subject.pastelBg
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
              ) {
                Icon(
                  imageVector = subject.icon,
                  contentDescription = null,
                  tint = subject.accentColor,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = subject.displayName,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = subject.accentColor
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.surfaceVariant,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
              Text(
                text = "${difficulty.displayName} Depth",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  letterSpacing = 0.4.sp
                ),
                color = EditorialMuted,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }

          IconButton(
            onClick = onResetSession,
            modifier = Modifier.testTag("reset_tutor_session_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Restart Session",
              tint = EditorialMuted,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Topic title
        Text(
          text = activeTopic,
          style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Learning Mode Selector Pills
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(LearningMode.values()) { mode ->
            val isSelected = mode == learningMode
            val modeBg by animateColorAsState(
              targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              label = "mode_pill_bg"
            )
            val modeTextColor by animateColorAsState(
              targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else EditorialMuted,
              label = "mode_pill_text"
            )

            Surface(
              shape = RoundedCornerShape(50),
              color = modeBg,
              border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
              modifier = Modifier
                .clickable { onLearningModeChange(mode) }
                .testTag("tutor_mode_pill_${mode.name.lowercase()}")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = mode.icon,
                  contentDescription = null,
                  tint = modeTextColor,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = mode.displayName,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                  ),
                  color = modeTextColor
                )
              }
            }
          }
        }
      }
    }

    // Chat Message Thread or Empty State
    if (messages.isEmpty()) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Psychology,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(30.dp)
            )
          }
          Text(
            text = "Begin Socratic Exploration",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Ask a question, share an intuition, or pick a suggested concept to start guided discovery.",
            style = MaterialTheme.typography.bodySmall,
            color = EditorialMuted,
            textAlign = TextAlign.Center
          )
          Button(
            onClick = { onSendMessage("Can you help me understand the core principle step by step?") },
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Start Dialogue")
          }
        }
      }
    } else {
      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .testTag("ai_tutor_messages_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        items(messages) { message ->
          ChatBubbleItem(
            message = message,
            currentMode = learningMode,
            onQuickAnswerSelected = { answer ->
              onSendMessage(answer)
            },
            onCopyText = { text ->
              clipboardManager.setText(AnnotatedString(text))
              onShowSnackbar("Copied to clipboard")
            }
          )
        }

        if (isThinking) {
          item {
            SocraticReasoningStepIndicator(learningMode = learningMode)
          }
        }
      }
    }

    // Interactive quick thought prompts from the latest tutor question
    val latestMessage = messages.lastOrNull { it.sender == MessageSender.TUTOR }
    if (!isThinking && latestMessage?.suggestedAnswers?.isNotEmpty() == true) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 2.dp)
      ) {
        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(latestMessage.suggestedAnswers) { suggestion ->
            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier
                .clickable {
                  onSendMessage(suggestion)
                }
                .testTag("suggestion_chip_${suggestion.take(10).replace(" ", "_").lowercase()}")
            ) {
              Text(
                text = suggestion,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Medium,
                  letterSpacing = 0.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
              )
            }
          }
        }
      }
    }

    // Quick Action Buttons below AI Chat interface
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
        .padding(vertical = 4.dp)
        .testTag("quick_actions_container")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Bolt,
          contentDescription = null,
          tint = EditorialPrimary,
          modifier = Modifier.size(13.dp)
        )
        Text(
          text = "QUICK ACTIONS",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            fontSize = 10.sp
          ),
          color = EditorialMuted
        )
      }

      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("quick_actions_row"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(QuickAction.values()) { action ->
          val isEnabled = !isThinking
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(
              1.dp,
              if (isEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .clickable(enabled = isEnabled) {
                onSendMessage(action.promptText)
              }
              .testTag(action.testTag)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = if (isEnabled) MaterialTheme.colorScheme.primary else EditorialMuted,
                modifier = Modifier.size(15.dp)
              )
              Text(
                text = action.label,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 12.sp
                ),
                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else EditorialMuted
              )
            }
          }
        }
      }
    }

    // Input Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = {
            Text(
              text = if (isThinking) "Socratic tutor is reflecting..." else "Respond with your reasoning or ask why...",
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
              color = EditorialMuted
            )
          },
          enabled = !isThinking,
          shape = RoundedCornerShape(20.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
          ),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
          keyboardActions = KeyboardActions(onSend = {
            if (inputText.isNotBlank() && !isThinking) {
              val toSend = inputText
              inputText = ""
              focusManager.clearFocus()
              onSendMessage(toSend)
            }
          }),
          modifier = Modifier
            .weight(1f)
            .testTag("ai_tutor_input_field")
        )

        IconButton(
          onClick = {
            if (inputText.isNotBlank() && !isThinking) {
              val toSend = inputText
              inputText = ""
              focusManager.clearFocus()
              onSendMessage(toSend)
            }
          },
          enabled = !isThinking && inputText.isNotBlank(),
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
              if (!isThinking && inputText.isNotBlank()) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant
            )
            .testTag("send_reasoning_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send Response",
            tint = if (!isThinking && inputText.isNotBlank()) Color.White else EditorialMuted,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun SocraticReasoningStepIndicator(learningMode: LearningMode) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse_reasoning")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(800),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )

  var stepStage by remember { mutableIntStateOf(0) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1200)
      stepStage = (stepStage + 1) % 3
    }
  }

  val stageMessage = when (stepStage) {
    0 -> "Analyzing reasoning deduction..."
    1 -> "Synthesizing pedagogical step..."
    else -> when (learningMode) {
      LearningMode.SOCRATIC -> "Formulating guiding question..."
      LearningMode.HINT -> "Preparing subtle progressive hint..."
      LearningMode.EXPLAIN -> "Composing first-principles explanation..."
      LearningMode.CHALLENGE -> "Constructing counter-example challenge..."
    }
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("tutor_thinking_indicator"),
    horizontalArrangement = Arrangement.Start
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(EditorialPrimary),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = learningMode.icon,
        contentDescription = "AI Tutor",
        tint = Color.White,
        modifier = Modifier.size(18.dp)
      )
    }
    Spacer(modifier = Modifier.width(10.dp))
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      shadowElevation = 1.dp
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(16.dp),
          color = EditorialPrimary,
          strokeWidth = 2.dp
        )
        Text(
          text = stageMessage,
          style = MaterialTheme.typography.bodySmall.copy(
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp
          ),
          color = EditorialPrimary.copy(alpha = pulseAlpha)
        )
      }
    }
  }
}

@Composable
private fun ChatBubbleItem(
  message: ChatMessage,
  currentMode: LearningMode,
  onQuickAnswerSelected: (String) -> Unit,
  onCopyText: (String) -> Unit = {}
) {
  val isTutor = message.sender == MessageSender.TUTOR

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isTutor) Arrangement.Start else Arrangement.End
  ) {
    if (isTutor) {
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(EditorialPrimary),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = currentMode.icon,
          contentDescription = "AI Tutor",
          tint = Color.White,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
    }

    Column(
      horizontalAlignment = if (isTutor) Alignment.Start else Alignment.End,
      modifier = Modifier.widthIn(max = 310.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(
          topStart = 18.dp,
          topEnd = 18.dp,
          bottomStart = if (isTutor) 4.dp else 18.dp,
          bottomEnd = if (isTutor) 18.dp else 4.dp
        ),
        color = if (isTutor) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary,
        border = if (isTutor) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        shadowElevation = if (isTutor) 1.dp else 2.dp
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          if (isTutor) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(bottom = 6.dp)
              ) {
                Icon(
                  imageVector = currentMode.icon,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(12.dp)
                )
                Text(
                  text = currentMode.shortTag.uppercase(),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                  ),
                  color = MaterialTheme.colorScheme.primary
                )
              }

              IconButton(
                onClick = { onCopyText(message.text) },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = "Copy message",
                  tint = EditorialMuted,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
          }

          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium.copy(
              lineHeight = 21.sp,
              fontSize = 14.sp
            ),
            color = if (isTutor) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
          )

          // Key Guided Question / Hint / Challenge highlight box
          if (message.guidedQuestion != null) {
            Spacer(modifier = Modifier.height(10.dp))
            val boxBg = if (message.isKeyInsight) EditorialSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer
            val boxBorder = if (message.isKeyInsight) EditorialSuccess.copy(alpha = 0.4f) else EditorialUnderlineHighlight

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = boxBg,
              border = BorderStroke(1.dp, boxBorder)
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
              ) {
                Icon(
                  imageVector = if (message.isKeyInsight) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.HelpOutline,
                  contentDescription = null,
                  tint = if (message.isKeyInsight) EditorialSuccess else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = message.guidedQuestion,
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                  ),
                  color = if (message.isKeyInsight) EditorialSuccess else MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = message.timestamp,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = EditorialMuted,
        modifier = Modifier.padding(horizontal = 4.dp)
      )
    }
  }
}
