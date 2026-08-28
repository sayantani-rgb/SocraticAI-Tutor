package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MessageSender {
  TUTOR,
  STUDENT
}

data class ChatMessage(
  val id: String = System.currentTimeMillis().toString() + "_" + (100..999).random(),
  val sender: MessageSender,
  val text: String,
  val timestamp: String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
  val guidedQuestion: String? = null,
  val suggestedAnswers: List<String> = emptyList(),
  val isKeyInsight: Boolean = false
)

data class DiscoveryMilestone(
  val title: String,
  val description: String,
  val isAchieved: Boolean = false
)
