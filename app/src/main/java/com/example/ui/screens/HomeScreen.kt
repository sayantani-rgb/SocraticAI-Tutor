package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Difficulty
import com.example.model.LearningMode
import com.example.model.Subject
import com.example.ui.theme.*

@Composable
fun HomeScreen(
  selectedSubject: Subject,
  onSubjectSelected: (Subject) -> Unit,
  selectedDifficulty: Difficulty,
  onDifficultySelected: (Difficulty) -> Unit,
  selectedLearningMode: LearningMode = LearningMode.SOCRATIC,
  onLearningModeSelected: (LearningMode) -> Unit = {},
  questionInput: String,
  onQuestionInputChange: (String) -> Unit,
  onStartLearning: () -> Unit,
  modifier: Modifier = Modifier
) {
  val focusManager = LocalFocusManager.current

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("home_screen_container"),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(22.dp)
  ) {
    // 1. Editorial Hero Landing Section
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Tagline badge
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.padding(bottom = 2.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = "GUIDED INQUIRY TUTOR",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 10.sp
              ),
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        // Hero Headline
        Text(
          text = buildAnnotatedString {
            append("Discover truth through ")
            withStyle(
              style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic
              )
            ) {
              append("curiosity")
            }
            append(" & guided reasoning.")
          },
          style = MaterialTheme.typography.headlineLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp,
            fontSize = 28.sp
          ),
          color = MaterialTheme.colorScheme.onBackground
        )

        // Subtitle
        Text(
          text = "Choose your subject, difficulty, and learning mode to explore first principles with AI Socratic tutoring.",
          style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 20.sp,
            fontSize = 14.sp
          ),
          color = EditorialSecondary
        )
      }
    }

    // 2. Subject Selector Section
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "SELECT FIELD OF INQUIRY",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.4.sp
            ),
            color = EditorialMuted
          )

          Surface(
            shape = RoundedCornerShape(50),
            color = selectedSubject.pastelBg
          ) {
            Text(
              text = selectedSubject.displayName.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              ),
              color = selectedSubject.accentColor,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }

        // Editorial Subject Grid
        val subjects = Subject.values()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          val chunkedSubjects = subjects.toList().chunked(3)
          chunkedSubjects.forEach { rowSubjects ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              rowSubjects.forEach { subject ->
                val isSelected = subject == selectedSubject
                EditorialSubjectCard(
                  subject = subject,
                  isSelected = isSelected,
                  onClick = { onSubjectSelected(subject) },
                  modifier = Modifier.weight(1f)
                )
              }
              if (rowSubjects.size < 3) {
                repeat(3 - rowSubjects.size) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }
    }

    // 3. Difficulty Level Section
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "DIFFICULTY LEVEL",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
          ),
          color = EditorialMuted
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Difficulty.values().forEach { difficulty ->
            val isSelected = difficulty == selectedDifficulty
            EditorialDifficultyPill(
              difficulty = difficulty,
              isSelected = isSelected,
              onClick = { onDifficultySelected(difficulty) },
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // 4. Learning Mode Section
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "LEARNING MODE",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.4.sp
            ),
            color = EditorialMuted
          )

          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer
          ) {
            Text(
              text = selectedLearningMode.badgeLabel.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontSize = 10.sp
              ),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }

        // Row of 4 Learning Modes
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("home_learning_mode_selector"),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          LearningMode.values().forEach { mode ->
            val isSelected = mode == selectedLearningMode
            EditorialLearningModePill(
              mode = mode,
              isSelected = isSelected,
              onClick = { onLearningModeSelected(mode) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        // Descriptive helper text for active mode
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = selectedLearningMode.icon,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = selectedLearningMode.description,
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 15.sp
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // 5. Inquiry / Topic Input Section & Start Button
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "TOPIC OF INQUIRY",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
          ),
          color = EditorialMuted
        )

        // Warm parchment input container with right search icon badge
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              OutlinedTextField(
                value = questionInput,
                onValueChange = onQuestionInputChange,
                placeholder = {
                  Text(
                    text = "What are you curious about today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EditorialMuted
                  )
                },
                trailingIcon = {
                  if (questionInput.isNotEmpty()) {
                    IconButton(
                      onClick = { onQuestionInputChange("") },
                      modifier = Modifier.testTag("clear_question_button")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear input",
                        tint = EditorialMuted
                      )
                    }
                  }
                },
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = Color.Transparent,
                  unfocusedContainerColor = Color.Transparent,
                  focusedBorderColor = Color.Transparent,
                  unfocusedBorderColor = Color.Transparent,
                  cursorColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                  focusManager.clearFocus()
                  if (questionInput.isNotBlank()) onStartLearning()
                }),
                modifier = Modifier
                  .weight(1f)
                  .testTag("topic_question_input")
              )

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.size(38.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }

            // Quick Subject Prompts Chips
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(selectedSubject.sampleQuestions) { sample ->
                Surface(
                  shape = RoundedCornerShape(50),
                  color = MaterialTheme.colorScheme.surface,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                  modifier = Modifier
                    .clickable { onQuestionInputChange(sample) }
                    .testTag("suggestion_chip_${sample.hashCode()}")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Lightbulb,
                      contentDescription = null,
                      modifier = Modifier.size(13.dp),
                      tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                      text = sample,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp
                      ),
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1
                    )
                  }
                }
              }
            }
          }
        }

        // Full-Width Primary Action "START LEARNING" Button
        Button(
          onClick = {
            focusManager.clearFocus()
            onStartLearning()
          },
          shape = RoundedCornerShape(18.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 3.dp,
            pressedElevation = 6.dp
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("start_learning_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Psychology,
              contentDescription = null,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = "START LEARNING",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
              )
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }

    // 6. Socratic Method Pillars Section
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "THE SOCRATIC METHOD",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
          ),
          color = EditorialMuted
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          EditorialPillarCard(
            title = "Guided Inquiry",
            description = "Progressive, thought-provoking questions tailored to you",
            icon = Icons.Default.QuestionAnswer,
            modifier = Modifier.weight(1f)
          )
          EditorialPillarCard(
            title = "Active Discovery",
            description = "Synthesize principles and connect your own insights",
            icon = Icons.Default.Lightbulb,
            modifier = Modifier.weight(1f)
          )
          EditorialPillarCard(
            title = "Deep Intuition",
            description = "Grasp foundational mechanics rather than rote memorization",
            icon = Icons.Default.Psychology,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

@Composable
private fun EditorialSubjectCard(
  subject: Subject,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
  val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else EditorialOnSurfaceVariant
  val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = containerColor,
    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
    shadowElevation = if (isSelected) 2.dp else 0.dp,
    modifier = modifier
      .height(88.dp)
      .clickable(onClick = onClick)
      .testTag("subject_card_${subject.name.lowercase()}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = subject.icon,
          contentDescription = subject.displayName,
          tint = if (isSelected) MaterialTheme.colorScheme.primary else EditorialSecondary,
          modifier = Modifier.size(19.dp)
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = subject.displayName,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
          fontSize = 11.sp
        ),
        color = contentColor,
        textAlign = TextAlign.Center,
        maxLines = 1
      )
    }
  }
}

@Composable
private fun EditorialDifficultyPill(
  difficulty: Difficulty,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
  val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else EditorialSecondary
  val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

  Surface(
    shape = RoundedCornerShape(50),
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
    shadowElevation = if (isSelected) 2.dp else 0.dp,
    modifier = modifier
      .height(42.dp)
      .clickable(onClick = onClick)
      .testTag("difficulty_card_${difficulty.name.lowercase()}")
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = difficulty.displayName,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp,
          fontSize = 12.sp
        ),
        color = textColor,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun EditorialLearningModePill(
  mode: LearningMode,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
  val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
  val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
    shadowElevation = if (isSelected) 2.dp else 0.dp,
    modifier = modifier
      .height(46.dp)
      .clickable(onClick = onClick)
      .testTag("home_learning_mode_${mode.name.lowercase()}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = mode.icon,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = mode.displayName,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
          fontSize = 11.sp
        ),
        color = contentColor,
        textAlign = TextAlign.Center,
        maxLines = 1
      )
    }
  }
}

@Composable
private fun EditorialPillarCard(
  title: String,
  description: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier.height(132.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(12.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Box(
        modifier = Modifier
          .size(30.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp)
        )
      }

      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 10.sp,
            lineHeight = 13.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 3
        )
      }
    }
  }
}
