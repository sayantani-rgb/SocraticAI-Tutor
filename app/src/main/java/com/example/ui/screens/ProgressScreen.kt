package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.Subject
import com.example.model.ThinkingSkillIndicator
import com.example.model.ThinkingSkillType
import com.example.progress.ThinkingSkillsRepository
import com.example.ui.theme.*

@Composable
fun ProgressScreen(
  modifier: Modifier = Modifier,
  conversationHistory: List<ChatMessage> = emptyList(),
  onExploreSkillInTutor: (Subject, String) -> Unit = { _, _ -> },
  onTakeSkillQuiz: (Subject, String) -> Unit = { _, _ -> }
) {
  val skillsRepository = remember { ThinkingSkillsRepository() }
  val thinkingProfile = remember(conversationHistory) {
    skillsRepository.calculateThinkingSkills(conversationHistory)
  }

  var selectedSkillFilter by remember { mutableStateOf<ThinkingSkillType?>(null) }
  var showDisclaimerDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("progress_screen_container"),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    // 1. Editorial Learning Journey Banner
    item {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, EditorialUnderlineHighlight),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("progress_journey_banner")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(EditorialPrimary),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }

            Column {
              Text(
                text = "PROGRESS TRACKER",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.4.sp
                ),
                color = EditorialPrimary
              )
              Text(
                text = "Your Inquiry Milestones",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }

          Text(
            text = "“Your learning journey is just beginning! Keep exploring.”",
            style = MaterialTheme.typography.headlineSmall.copy(
              fontFamily = FontFamily.Serif,
              fontStyle = FontStyle.Italic,
              fontWeight = FontWeight.Normal,
              fontSize = 21.sp,
              lineHeight = 27.sp
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.testTag("progress_journey_message")
          )

          Text(
            text = "As you engage in deeper Socratic dialogues, this space tracks the concepts you investigate, active thinking streaks, and principles you uncover through guided questioning.",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontSize = 13.sp,
              lineHeight = 19.sp
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
          )
        }
      }
    }

    // 2. High-Level Summary Stats
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "INQUIRY METRICS",
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
          EditorialProgressStatBox(
            title = "Inquiries",
            value = "${thinkingProfile.totalInquiriesAnalyzed}",
            subtitle = "Dialogues held",
            icon = Icons.Default.Psychology,
            containerColor = MaterialTheme.colorScheme.surface,
            accentColor = EditorialPrimary,
            modifier = Modifier.weight(1f)
          )
          EditorialProgressStatBox(
            title = "Thinking Streak",
            value = "3 Days",
            subtitle = "Consecutive days",
            icon = Icons.Default.LocalFireDepartment,
            containerColor = MaterialTheme.colorScheme.surface,
            accentColor = EditorialTertiary,
            modifier = Modifier.weight(1f)
          )
          EditorialProgressStatBox(
            title = "Deductions",
            value = "6",
            subtitle = "Core principles",
            icon = Icons.Default.CheckCircle,
            containerColor = MaterialTheme.colorScheme.surface,
            accentColor = EditorialPrimary,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // ==========================================
    // 3. THINKING SKILLS SECTION (NEW)
    // ==========================================
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("thinking_skills_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Section Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "THINKING SKILLS",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
              ),
              color = EditorialPrimary
            )
            Text(
              text = "Estimated Learning Indicators",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
              ),
              color = MaterialTheme.colorScheme.onBackground
            )
          }

          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.dp, EditorialOutline)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = EditorialPrimary,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = "${thinkingProfile.overallIndex}% Index",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = EditorialPrimary
              )
            }
          }
        }

        // Prominent Methodology Notice & Disclaimer Card
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = EditorialTertiaryContainer.copy(alpha = 0.6f),
          border = BorderStroke(1.dp, EditorialTertiary.copy(alpha = 0.3f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("thinking_skills_disclaimer_card")
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Information",
                tint = EditorialTertiary,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = "Formative Learning Estimates Notice",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.2.sp
                ),
                color = EditorialTertiary
              )
            }

            Text(
              text = "These are estimated learning indicators based on your activity, dialogue answers, and quiz performance within the Socratic application. They are designed for formative study guidance and are not scientifically accurate psychological or intelligence measurements.",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 17.sp
              ),
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
              modifier = Modifier.testTag("thinking_skills_disclaimer_text")
            )
          }
        }

        // Skill Filter Chips
        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          item {
            FilterChip(
              selected = selectedSkillFilter == null,
              onClick = { selectedSkillFilter = null },
              label = { Text("All 4 Skills", style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = EditorialPrimary,
                selectedLabelColor = Color.White
              ),
              border = FilterChipDefaults.filterChipBorder(
                borderColor = EditorialOutline,
                enabled = true,
                selected = selectedSkillFilter == null
              )
            )
          }

          items(ThinkingSkillType.values()) { skillType ->
            FilterChip(
              selected = selectedSkillFilter == skillType,
              onClick = {
                selectedSkillFilter = if (selectedSkillFilter == skillType) null else skillType
              },
              leadingIcon = {
                Icon(
                  imageVector = skillType.icon,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp)
                )
              },
              label = { Text(skillType.title, style = MaterialTheme.typography.labelSmall) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = EditorialPrimary,
                selectedLabelColor = Color.White
              ),
              border = FilterChipDefaults.filterChipBorder(
                borderColor = EditorialOutline,
                enabled = true,
                selected = selectedSkillFilter == skillType
              )
            )
          }
        }
      }
    }

    // 4. Individual Thinking Skill Indicator Cards
    val filteredIndicators = if (selectedSkillFilter != null) {
      thinkingProfile.indicators.filter { it.type == selectedSkillFilter }
    } else {
      thinkingProfile.indicators
    }

    items(filteredIndicators) { indicator ->
      ThinkingSkillCard(
        indicator = indicator,
        onExploreTutor = { subject, prompt ->
          onExploreSkillInTutor(subject, prompt)
        },
        onTakeQuiz = { subject, prompt ->
          onTakeSkillQuiz(subject, prompt)
        }
      )
    }

    // 5. Subject Exploration Breakdown (Retained from original)
    item {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Text(
            text = "Subject Exploration Distribution",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = (-0.2).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )

          val subjectStats = listOf(
            Triple(Subject.MATHEMATICS, 0.75f, "6 Dialogues"),
            Triple(Subject.PHYSICS, 0.50f, "4 Dialogues"),
            Triple(Subject.COMPUTER_SCIENCE, 0.35f, "3 Dialogues"),
            Triple(Subject.CHEMISTRY, 0.20f, "1 Dialogue")
          )

          subjectStats.forEach { (subj, prog, count) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = subj.icon,
                    contentDescription = null,
                    tint = EditorialPrimary,
                    modifier = Modifier.size(16.dp)
                  )
                  Text(
                    text = subj.displayName,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
                Text(
                  text = count,
                  style = MaterialTheme.typography.labelSmall,
                  color = EditorialMuted
                )
              }

              LinearProgressIndicator(
                progress = { prog },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(7.dp)
                  .clip(RoundedCornerShape(4.dp)),
                color = EditorialPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
              )
            }
          }
        }
      }
    }

    // 6. Recent Discovery Milestones (Retained from original)
    item {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Recent Discovery Milestones",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = (-0.2).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )

          val milestones = listOf(
            "Deduced why ice expands upon freezing via crystal lattice structure",
            "Discovered the intuitive geometric proof for negative multiplication",
            "Understood logarithmic division mechanics in binary search"
          )

          milestones.forEach { milestone ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = EditorialPrimary,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = milestone,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 13.sp,
                  lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Card rendering an individual Thinking Skill Indicator with score,
 * badge, feedback, suggestions, and action links.
 */
@Composable
private fun ThinkingSkillCard(
  indicator: ThinkingSkillIndicator,
  onExploreTutor: (Subject, String) -> Unit,
  onTakeQuiz: (Subject, String) -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(true) }

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier
      .fillMaxWidth()
      .animateContentSize()
      .testTag("thinking_skill_card_${indicator.type.name.lowercase()}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Top Header Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(indicator.type.containerColor),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = indicator.type.icon,
              contentDescription = null,
              tint = indicator.type.textColor,
              modifier = Modifier.size(22.dp)
            )
          }

          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = indicator.type.title,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = indicator.type.shortDescription,
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 15.sp
              ),
              color = EditorialMuted
            )
          }
        }

        // Mastery Badge
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = indicator.type.containerColor,
          border = BorderStroke(1.dp, indicator.type.textColor.copy(alpha = 0.2f))
        ) {
          Text(
            text = indicator.levelBadge,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp
            ),
            color = indicator.type.textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      // Mastery Score Progress Bar
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Estimated Proficiency",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${indicator.scorePercent}%",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            ),
            color = EditorialPrimary
          )
        }

        LinearProgressIndicator(
          progress = { indicator.scorePercent / 100f },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = indicator.type.textColor,
          trackColor = indicator.type.containerColor
        )

        Text(
          text = indicator.trendLabel,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          ),
          color = EditorialMuted
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // Primary Useful Feedback (Quote Style)
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = indicator.type.containerColor.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, indicator.type.textColor.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.Top
        ) {
          Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = indicator.type.textColor,
            modifier = Modifier
              .size(18.dp)
              .padding(top = 2.dp)
          )

          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "OBSERVED LEARNING PATTERN",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp
              ),
              color = indicator.type.textColor
            )
            Text(
              text = "“${indicator.primaryFeedback}”",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                lineHeight = 19.sp
              ),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.testTag("skill_feedback_${indicator.type.name.lowercase()}")
            )
          }
        }
      }

      // Actionable Suggestion & Next Step
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
      ) {
        Icon(
          imageVector = Icons.Default.Lightbulb,
          contentDescription = null,
          tint = EditorialTertiary,
          modifier = Modifier.size(18.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "Actionable Suggestion",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp
            ),
            color = EditorialTertiary
          )
          Text(
            text = indicator.actionableSuggestion,
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 12.sp,
              lineHeight = 17.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("skill_suggestion_${indicator.type.name.lowercase()}")
          )
        }
      }

      // Evidence Basis Footnote
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Analytics,
          contentDescription = null,
          tint = EditorialMuted,
          modifier = Modifier.size(14.dp)
        )
        Text(
          text = indicator.evidenceBasis,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
            color = EditorialMuted
          )
        )
      }

      // Practice Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = { onExploreTutor(indicator.recommendedSubject, indicator.practicePrompt) },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = EditorialPrimary
          ),
          border = BorderStroke(1.dp, EditorialOutline),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("btn_practice_tutor_${indicator.type.name.lowercase()}")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Psychology,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
            Text("Practice in Tutor", style = MaterialTheme.typography.labelSmall)
          }
        }

        FilledTonalButton(
          onClick = { onTakeQuiz(indicator.recommendedSubject, indicator.practicePrompt) },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = indicator.type.containerColor,
            contentColor = indicator.type.textColor
          ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("btn_take_quiz_${indicator.type.name.lowercase()}")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Quiz,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
            Text("Take Skill Quiz", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }
  }
}

@Composable
private fun EditorialProgressStatBox(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  containerColor: Color,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = containerColor,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(16.dp)
        )
      }
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        ),
        color = EditorialPrimary
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 9.sp,
          lineHeight = 12.sp
        ),
        color = EditorialMuted
      )
    }
  }
}
