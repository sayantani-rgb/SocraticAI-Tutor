package com.example.tutor

import android.util.Log
import com.example.BuildConfig
import com.example.model.ChatMessage
import com.example.model.Difficulty
import com.example.model.LearningMode
import com.example.model.MessageSender
import com.example.model.Subject
import com.example.network.GeminiClient
import com.example.network.GeminiContent
import com.example.network.GeminiGenerationConfig
import com.example.network.GeminiPart
import com.example.network.GeminiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SocraticTutorRepository {

  companion object {
    private const val TAG = "SocraticTutorRepo"
  }

  suspend fun startSession(
    subject: Subject,
    difficulty: Difficulty,
    topic: String,
    learningMode: LearningMode = LearningMode.SOCRATIC
  ): ChatMessage = withContext(Dispatchers.IO) {
    val cleanTopic = topic.trim().ifBlank { subject.sampleQuestions.first() }

    val modeInstructions = when (learningMode) {
      LearningMode.SOCRATIC -> "Initiate a Socratic inquiry: ask a welcoming observation and the first foundational guiding question to spark deduction."
      LearningMode.HINT -> "Initiate Hint Mode: briefly outline the problem and provide the first SUBTLE hint to get started."
      LearningMode.EXPLAIN -> "Initiate Explain Mode: provide a direct, simple definition and a clear real-world analogy explaining this concept, followed by a check question."
      LearningMode.CHALLENGE -> "Initiate Challenge Mode: present a rigorous, high-level problem or counter-intuitive edge case scenario that tests deep critical thinking."
    }

    val prompt = "The student wants to explore the concept: \"$cleanTopic\" in ${subject.displayName} at a ${difficulty.displayName} level in ${learningMode.displayName} Mode.\n$modeInstructions"

    val systemInstruction = buildSystemInstruction(subject, difficulty, learningMode)

    val apiKey = getApiKey()
    if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
      try {
        val request = GeminiRequest(
          contents = listOf(
            GeminiContent(
              role = "user",
              parts = listOf(GeminiPart(text = prompt))
            )
          ),
          systemInstruction = GeminiContent(
            parts = listOf(GeminiPart(text = systemInstruction))
          ),
          generationConfig = GeminiGenerationConfig(
            temperature = 0.7f,
            topP = 0.95f,
            maxOutputTokens = 1024
          )
        )

        val response = GeminiClient.api.generateContent(apiKey, request)
        val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        if (!textResponse.isNullOrBlank()) {
          return@withContext parseSocraticResponse(textResponse, subject, difficulty, cleanTopic, learningMode)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Gemini API error during session start: ${e.message}", e)
      }
    }

    // Heuristic Fallback adapted to LearningMode
    buildHeuristicInitialInquiry(subject, difficulty, cleanTopic, learningMode)
  }

  suspend fun sendStudentMessage(
    subject: Subject,
    difficulty: Difficulty,
    topic: String,
    history: List<ChatMessage>,
    studentInput: String,
    learningMode: LearningMode = LearningMode.SOCRATIC
  ): ChatMessage = withContext(Dispatchers.IO) {
    val cleanTopic = topic.trim().ifBlank { subject.sampleQuestions.first() }
    val systemInstruction = buildSystemInstruction(subject, difficulty, learningMode)

    val apiKey = getApiKey()
    if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
      try {
        val geminiContents = mutableListOf<GeminiContent>()

        // System context reminder
        geminiContents.add(
          GeminiContent(
            role = "user",
            parts = listOf(
              GeminiPart(
                text = "We are in an ongoing learning session in ${learningMode.displayName} Mode exploring: \"$cleanTopic\" in ${subject.displayName} (${difficulty.displayName} level). Track the dialogue history carefully. Evaluate whether the student's latest response shows understanding, a misconception, or struggling, and adapt your response strictly to ${learningMode.displayName} Mode."
              )
            )
          )
        )

        // Convert prior history
        for (msg in history.takeLast(12)) {
          val role = if (msg.sender == MessageSender.TUTOR) "model" else "user"
          val contentText = buildString {
            append(msg.text)
            if (!msg.guidedQuestion.isNullOrBlank()) {
              append("\n[GUIDED_QUESTION] ")
              append(msg.guidedQuestion)
            }
          }
          geminiContents.add(
            GeminiContent(
              role = role,
              parts = listOf(GeminiPart(text = contentText))
            )
          )
        }

        // Add latest student message
        geminiContents.add(
          GeminiContent(
            role = "user",
            parts = listOf(GeminiPart(text = studentInput))
          )
        )

        val request = GeminiRequest(
          contents = geminiContents,
          systemInstruction = GeminiContent(
            parts = listOf(GeminiPart(text = systemInstruction))
          ),
          generationConfig = GeminiGenerationConfig(
            temperature = 0.7f,
            topP = 0.95f,
            maxOutputTokens = 1024
          )
        )

        val response = GeminiClient.api.generateContent(apiKey, request)
        val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        if (!textResponse.isNullOrBlank()) {
          return@withContext parseSocraticResponse(textResponse, subject, difficulty, cleanTopic, learningMode)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Gemini API error during chat turn: ${e.message}", e)
      }
    }

    // Intelligent Heuristic Fallback
    buildIntelligentHeuristicFollowUp(subject, difficulty, cleanTopic, history, studentInput, learningMode)
  }

  private fun getApiKey(): String {
    return try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Exception) {
      ""
    }
  }

  private fun buildSystemInstruction(
    subject: Subject,
    difficulty: Difficulty,
    learningMode: LearningMode
  ): String {
    val modeGuidelines = when (learningMode) {
      LearningMode.SOCRATIC -> """
=== CURRENT MODE: SOCRATIC MODE (DEFAULT) ===
- Primarily teach through guiding questions.
- Strictly avoid revealing answers directly.
- Guide the student step by step to construct their own insight.
- When they make a mistake: Gently identify the misconception, ask them to reconsider that specific part, and provide a simpler guiding question.
"""
      LearningMode.HINT -> """
=== CURRENT MODE: HINT MODE ===
- Provide progressive, gradual hints to help the student solve the problem.
- Turn 1 / Initial hint: Give a SUBTLE hint (e.g. "Hint 1: Look at the relationship between mass and volume...").
- Subsequent turns / If struggling: Provide more detailed, intermediate hints (e.g. "Hint 2: Ice expands when freezing, which increases its volume while keeping mass constant...").
- Only give the direct answer if the student has exhausted hints and explicitly asks.
- Ask one guided question at the end to prompt them to apply the hint.
"""
      LearningMode.EXPLAIN -> """
=== CURRENT MODE: EXPLAIN MODE ===
- Directly and comprehensively explain the concept.
- Use simple, accessible definitions, vivid real-world analogies, and clear step-by-step explanations.
- In [EXPLANATION], provide the full conceptual walkthrough clearly and concisely.
- In [GUIDED_QUESTION], ask a quick check question to verify that the student understood the key principle.
"""
      LearningMode.CHALLENGE -> """
=== CURRENT MODE: CHALLENGE MODE ===
- Present demanding, rigorous questions testing deeper conceptual understanding, critical thinking, edge cases, and non-trivial problem-solving.
- Challenge the student's initial assumptions and probe for deeper mathematical/logical precision.
- In [GUIDED_QUESTION], ask a thought-provoking challenge scenario or counter-intuitive thought experiment.
"""
    }

    return """
You are the master AI Tutor for the 'Socratic' Android learning platform. You are teaching ${subject.displayName} at a ${difficulty.displayName} level.

$modeGuidelines

Universal Pedagogical Principles:
1. CONTEXT MEMORY: Retain complete awareness of the ongoing session, previous turns, and student deductions.
2. RIGOROUS UNDERSTANDING ANALYSIS:
   - Analyze whether the student's answer actually demonstrates valid understanding or a misconception.
   - Do NOT pretend the student understands something unless they have demonstrated it.
3. GENTLE MISTAKE CORRECTION:
   - Never say "Wrong" or dismissive words.
   - Step 1: Gently identify the possible mistake or faulty premise.
   - Step 2: Ask the student to reconsider that specific part.
   - Step 3: Provide a guiding question.
   - Step 4: Give the student another opportunity to answer.
4. ADAPTIVE PROGRESSION:
   - Use simpler questions and everyday analogies when the student is struggling.
   - Gradually increase difficulty when the student demonstrates mastery.
5. DISCIPLINE:
   - Ask strictly ONE meaningful question at a time.
   - Keep tone warm, encouraging, engaging, and intellectual.
6. QUICK ACTION INSTRUCTION RESPONSIVENESS:
   - If the student asks "Give me a hint": Provide a subtle, targeted hint that points in the right direction without immediately revealing the full answer.
   - If the student asks "Guide me step by step": Break down the problem or deduction into clear, incremental numbered steps and guide them through the next step.
   - If the student asks "Help me understand my mistake": Gently explain where common misunderstandings or pitfalls occur, clarifying the underlying logic without discouragement.
   - If the student asks "Explain with an example": Provide a vivid, practical, easy-to-understand real-world example, relatable metaphor, or clear code snippet.
   - If the student asks "Make it easier": Simplify the explanation into plain language with everyday words, removing unnecessary complexity or jargon.
   - If the student asks "Challenge me": Present a thought-provoking, higher-order problem, counter-intuitive puzzle, or edge-case scenario.
   - If the student asks "Give me the answer": Provide the complete, definitive answer with a full conceptual explanation and summary.

Output Format strictly as:
[EXPLANATION]
(Concise response aligned with the current mode—acknowledging thoughts, giving hints, explaining directly, or posing challenge analysis)

[GUIDED_QUESTION]
(Exactly ONE focused question matching the current mode)

[SUGGESTIONS]
- (Interactive suggestion 1)
- (Interactive suggestion 2)
- (Interactive suggestion 3)
""".trimIndent()
  }

  private fun parseSocraticResponse(
    raw: String,
    subject: Subject,
    difficulty: Difficulty,
    topic: String,
    learningMode: LearningMode
  ): ChatMessage {
    var explanation = ""
    var guidedQuestion = ""
    val suggestions = mutableListOf<String>()

    if (raw.contains("[EXPLANATION]") || raw.contains("[GUIDED_QUESTION]")) {
      val explanationMatch = Regex("""\[EXPLANATION\]\s*([\s\S]*?)(?=\[GUIDED_QUESTION\]|$)""").find(raw)
      val questionMatch = Regex("""\[GUIDED_QUESTION\]\s*([\s\S]*?)(?=\[SUGGESTIONS\]|$)""").find(raw)
      val suggestionsMatch = Regex("""\[SUGGESTIONS\]\s*([\s\S]*)""").find(raw)

      explanation = explanationMatch?.groupValues?.get(1)?.trim() ?: ""
      guidedQuestion = questionMatch?.groupValues?.get(1)?.trim() ?: ""

      val suggestionsBlock = suggestionsMatch?.groupValues?.get(1)?.trim() ?: ""
      if (suggestionsBlock.isNotBlank()) {
        suggestionsBlock.lines().forEach { line ->
          val cleaned = line.trim().removePrefix("-").removePrefix("*").removePrefix("•").trim()
          if (cleaned.isNotBlank()) {
            suggestions.add(cleaned)
          }
        }
      }
    }

    if (explanation.isBlank() && guidedQuestion.isBlank()) {
      val parts = raw.split("?")
      if (parts.size >= 2) {
        explanation = parts.dropLast(1).dropLast(1).joinToString("?") + if (parts.size > 2) "?" else ""
        if (explanation.isBlank()) explanation = parts.first().trim()
        guidedQuestion = parts[parts.size - 2].trim().substringAfterLast("\n") + "?"
      } else {
        explanation = raw.trim()
        guidedQuestion = when (learningMode) {
          LearningMode.EXPLAIN -> "How would you summarize this principle in your own words?"
          LearningMode.HINT -> "Does this hint help you see how the pieces fit together?"
          LearningMode.CHALLENGE -> "How would this hold up in an extreme edge-case scenario?"
          LearningMode.SOCRATIC -> "What comes to mind when considering this perspective?"
        }
      }
    } else if (guidedQuestion.isBlank()) {
      guidedQuestion = "How do you see this connecting to our topic of $topic?"
    }

    if (suggestions.isEmpty()) {
      when (learningMode) {
        LearningMode.HINT -> {
          suggestions.add("Give me another hint")
          suggestions.add("I think I see the answer now")
          suggestions.add("Explain this part a bit more")
        }
        LearningMode.EXPLAIN -> {
          suggestions.add("I understand the explanation")
          suggestions.add("Give me a concrete code example")
          suggestions.add("How does this apply in practice?")
        }
        LearningMode.CHALLENGE -> {
          suggestions.add("Let's analyze the edge cases")
          suggestions.add("Here is my hypothesis...")
          suggestions.add("What if conditions are inverted?")
        }
        LearningMode.SOCRATIC -> {
          suggestions.add("Explain further with an example")
          suggestions.add("I see how that connects")
          suggestions.add("Let's look at the next step")
        }
      }
    }

    val isKeyInsight = raw.contains("congratulations", ignoreCase = true) ||
        raw.contains("deduced", ignoreCase = true) ||
        raw.contains("mastered", ignoreCase = true) ||
        raw.contains("brilliant deduction", ignoreCase = true) ||
        raw.contains("challenge mastered", ignoreCase = true)

    return ChatMessage(
      sender = MessageSender.TUTOR,
      text = explanation.ifBlank { "Let's examine this carefully." },
      guidedQuestion = guidedQuestion,
      suggestedAnswers = suggestions.take(3),
      isKeyInsight = isKeyInsight
    )
  }

  private fun buildHeuristicInitialInquiry(
    subject: Subject,
    difficulty: Difficulty,
    topic: String,
    learningMode: LearningMode
  ): ChatMessage {
    val topicLower = topic.lowercase()
    val isPython = topicLower.contains("python") || topicLower.contains("function")
    val isIce = topicLower.contains("ice") || topicLower.contains("float") || topicLower.contains("density")

    val (intro, question, suggestions) = when (learningMode) {
      LearningMode.EXPLAIN -> {
        if (isPython) {
          Triple(
            "In Python, a function is a named, reusable block of code that takes optional inputs (parameters), executes a set of instructions, and returns an output (return value). Think of it like a coffee machine: you insert beans and water (inputs), press brew, and receive coffee (output).",
            "Why is defining a function once better than copying the same code 10 times in a program?",
            listOf(
              "It avoids duplication and simplifies maintenance",
              "Show me basic Python function syntax",
              "What is the difference between print and return?"
            )
          )
        } else if (isIce) {
          Triple(
            "Ice floats on water because of its molecular structure: as liquid water cools below 4°C, hydrogen bonds organize water molecules into a spacious hexagonal crystal lattice. This expansion increases volume while mass remains unchanged, making solid ice approximately 9% less dense than liquid water.",
            "Since density equals mass divided by volume, how does an increase in volume cause ice to float?",
            listOf(
              "Greater volume for same mass means lower density than liquid water",
              "Why do most other substances shrink when solidifying?",
              "How does this protect marine life in winter?"
            )
          )
        } else {
          Triple(
            "Here is the foundational breakdown of “$topic” in ${subject.displayName}: Every system is defined by its core inputs, internal transformation rules, and resulting outputs.",
            "Would you like a step-by-step example or a real-world analogy to solidify this?",
            listOf(
              "Provide a step-by-step example",
              "Give me a real-world analogy",
              "Let's test my comprehension with a question"
            )
          )
        }
      }

      LearningMode.HINT -> {
        if (isPython) {
          Triple(
            "Welcome to Hint Mode for Python Functions! We will break down this concept with progressive clues.",
            "💡 Hint 1: Think about the 'DRY' principle in software engineering (Don't Repeat Yourself). What tool in Python encapsulates repetitive logic into a single named block?",
            listOf(
              "Functions (defined using def)",
              "Give me another hint",
              "I'm not sure yet"
            )
          )
        } else if (isIce) {
          Triple(
            "Welcome to Hint Mode for Buoyancy and Density!",
            "💡 Hint 1: Whether an object floats depends not on its raw mass, but on its density (mass per volume) compared to water.",
            listOf(
              "Ice must have lower density than water",
              "Give me hint #2",
              "How does temperature affect volume?"
            )
          )
        } else {
          Triple(
            "Welcome to Hint Mode for “$topic” in ${subject.displayName}!",
            "💡 Hint 1: Focus on the fundamental conservation rule or governing equation for this system.",
            listOf(
              "I have a guess based on Hint 1",
              "Give me the next hint",
              "Can you simplify the hint?"
            )
          )
        }
      }

      LearningMode.CHALLENGE -> {
        if (isPython) {
          Triple(
            "Challenge Mode engaged! Let's test non-trivial edge cases in Python.",
            "Consider a Python function with a default parameter: `def add_item(val, items=[])`. If you call `add_item(1)` then `add_item(2)`, what will `items` contain, and why does this mutable default argument quirk occur?",
            listOf(
              "It contains [1, 2] because the list is evaluated once at definition time",
              "It creates a new empty list each time",
              "How do we safely write default arguments?"
            )
          )
        } else if (isIce) {
          Triple(
            "Challenge Mode engaged! Let's test your conceptual rigor against a classic physics puzzle.",
            "A drinking glass is filled to the brim with liquid water and has an ice cube floating in it. When the ice cube melts completely, will the water level overflow, drop, or remain exactly unchanged? Why?",
            listOf(
              "It remains exactly unchanged because displaced mass equals melted mass",
              "It will overflow because ice expands",
              "The water level drops slightly"
            )
          )
        } else {
          Triple(
            "Challenge Mode engaged! Let's examine “$topic” under rigorous edge-case conditions in ${subject.displayName}.",
            "What happens to this system when one of the primary constraints approaches infinity or zero?",
            listOf(
              "It approaches an asymptotic limit",
              "The system undergoes a phase transition",
              "Let's break down the mathematical boundary"
            )
          )
        }
      }

      LearningMode.SOCRATIC -> {
        if (isPython) {
          Triple(
            "Welcome to Socratic inquiry on Python Functions. Rather than jumping straight to syntax, let's explore why software is structured this way.",
            "Why do you think programmers might want to avoid writing the same piece of code repeatedly?",
            listOf(
              "To make code reusable and easier to maintain",
              "To make the file smaller in size",
              "I'm not completely sure yet"
            )
          )
        } else if (isIce) {
          Triple(
            "Welcome! Most solids sink in their liquid state, yet ice floats on water.",
            "First, what fundamental relationship between an object's mass and its volume determines whether it floats in a liquid?",
            listOf(
              "Density: mass divided by volume compared to water",
              "The total weight of the object regardless of volume",
              "I'm not sure, how do mass and volume interact?"
            )
          )
        } else {
          Triple(
            "Welcome to our Socratic inquiry on $topic in ${subject.displayName} (${difficulty.displayName} level). Instead of rushing to a definition, let's explore its foundations.",
            "What is the most basic observation or real-world example you have seen related to this concept?",
            listOf(
              "Let's break down the basic components",
              "I'm completely new to this topic, can we start simpler?",
              "What problem does this concept solve?"
            )
          )
        }
      }
    }

    return ChatMessage(
      sender = MessageSender.TUTOR,
      text = intro,
      guidedQuestion = question,
      suggestedAnswers = suggestions
    )
  }

  private fun buildIntelligentHeuristicFollowUp(
    subject: Subject,
    difficulty: Difficulty,
    topic: String,
    history: List<ChatMessage>,
    studentInput: String,
    learningMode: LearningMode
  ): ChatMessage {
    val inputLower = studentInput.lowercase().trim()
    val turn = history.count { it.sender == MessageSender.STUDENT }
    val topicLower = topic.lowercase()
    val isPython = topicLower.contains("python") || topicLower.contains("function")
    val isIce = topicLower.contains("ice") || topicLower.contains("float") || topicLower.contains("density")

    // Explicit Quick Action Handlers
    val isGiveHint = inputLower.contains("give me a hint") || inputLower == "hint"
    val isStepByStep = inputLower.contains("guide me step by step") || inputLower.contains("step by step")
    val isUnderstandMistake = inputLower.contains("understand my mistake") || inputLower.contains("my mistake") || inputLower.contains("where is my mistake")
    val isExplainExample = inputLower.contains("explain with an example") || inputLower.contains("with an example") || inputLower.contains("give me an example") || inputLower.contains("practical example")
    val isMakeEasier = inputLower.contains("make it easier") || inputLower.contains("simpler") || inputLower.contains("make it simple")
    val isChallengeMe = inputLower.contains("challenge me") || inputLower == "challenge"
    val isGiveAnswer = inputLower.contains("give me the answer") || inputLower.contains("what is the answer") || inputLower == "give the answer"

    if (isGiveHint) {
      return if (isPython) {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "💡 **Progressive Hint**: Think about encapsulating instructions. In Python, you give a block of logic a name with `def`, specify what it receives in parentheses `(...)`, and use a keyword to send values back.",
          guidedQuestion = "What keyword in Python passes a calculated result back to the caller instead of just printing it?",
          suggestedAnswers = listOf(
            "The 'return' keyword",
            "Explain with an example",
            "Give me the answer"
          )
        )
      } else if (isIce) {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "💡 **Progressive Hint**: Density equals mass divided by volume ($\\rho = m / V$). Most liquids contract when solidifying, but water forms a hexagonal lattice with open crystalline pockets below 4°C.",
          guidedQuestion = "Because the frozen lattice expands the volume while mass stays the same, how does the density of ice compare to liquid water?",
          suggestedAnswers = listOf(
            "Ice is less dense than liquid water, so it floats",
            "Explain with an example",
            "Give me the answer"
          )
        )
      } else {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "💡 **Progressive Hint for $topic**: Notice the governing relationship between the system's input and how forces or rules balance out in ${subject.displayName}.",
          guidedQuestion = "What is the primary factor that causes the outcome to change in this system?",
          suggestedAnswers = listOf(
            "Guide me step by step",
            "Explain with an example",
            "Give me the answer"
          )
        )
      }
    }

    if (isStepByStep) {
      return if (isPython) {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "🧭 **Step-by-Step Breakdown for Python Functions**:\n\n" +
              "**Step 1: Declaration** — Define the function using `def name(parameters):`.\n" +
              "**Step 2: Parameters** — Pass input data into the parentheses.\n" +
              "**Step 3: Execution Body** — Write indented statements that perform calculations.\n" +
              "**Step 4: Return Statement** — Use `return result` to send data back.\n" +
              "**Step 5: Invocation** — Call the function where needed, like `output = name(args)`.",
          guidedQuestion = "Which step would you like to explore deeper or practice with a coding snippet?",
          suggestedAnswers = listOf(
            "Step 4: Return vs Print",
            "Explain with an example",
            "Challenge me"
          )
        )
      } else if (isIce) {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "🧭 **Step-by-Step Breakdown for Why Ice Floats**:\n\n" +
              "**Step 1: Cooling** — As liquid water cools, molecules slow down and get closer until 4°C.\n" +
              "**Step 2: Crystallization** — Below 4°C, hydrogen bonds organize water molecules into rigid hexagonal rings.\n" +
              "**Step 3: Expansion** — The hexagonal geometry leaves empty space, expanding volume by ~9%.\n" +
              "**Step 4: Lower Density** — Same mass in 9% more space means density drops from 1.00 to ~0.92 g/cm³.\n" +
              "**Step 5: Positive Buoyancy** — By Archimedes' Principle, the buoyant force exceeds the weight of ice, floating it.",
          guidedQuestion = "Does seeing this sequential chain from molecules to buoyancy make the phenomenon clear?",
          suggestedAnswers = listOf(
            "Yes, the volume expansion makes complete sense!",
            "Explain with an example",
            "Challenge me"
          )
        )
      } else {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "🧭 **Step-by-Step Walkthrough for $topic**:\n\n" +
              "**Step 1: Initial State** — Identify the inputs, givens, and constraints.\n" +
              "**Step 2: Governing Rule** — State the foundational equation or law in ${subject.displayName}.\n" +
              "**Step 3: Transformation** — Trace how variables interact incrementally.\n" +
              "**Step 4: Conclusion** — Deduce the resulting state or output.",
          guidedQuestion = "Starting with Step 1: what are the primary variables or conditions we need to consider?",
          suggestedAnswers = listOf(
            "Explain with an example",
            "Make it easier",
            "Give me the answer"
          )
        )
      }
    }

    if (isUnderstandMistake) {
      return if (isPython) {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "🔍 **Understanding Common Pitfalls with Functions**:\n\n" +
              "1. **`print` vs `return`**: `print()` only outputs characters to your terminal screen for human eyes. It cannot pass data to other functions. `return` hands real data back to your code.\n" +
              "2. **Scope Confusion**: Variables defined inside a function only exist inside that function; they disappear when it finishes.\n" +
              "3. **Missing Arguments**: Calling a function without required parameters causes a TypeError.",
          guidedQuestion = "Which of these common confusion points matches what you were thinking?",
          suggestedAnswers = listOf(
            "I was confusing print with return",
            "I wasn't sure how scope works",
            "Explain with an example"
          )
        )
      } else if (isIce) {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "🔍 **Understanding Common Pitfalls with Density & Buoyancy**:\n\n" +
              "1. **Weight vs Density**: People often assume heavy things sink and light things float. But a 100,000-ton aircraft carrier floats while a 5-gram pebble sinks! Floating depends on *density* (mass per unit volume), not raw weight.\n" +
              "2. **Solid = Denser Assumption**: For almost all other liquids, the solid state is denser. Water is uniquely anomalous because its hydrogen bonds create open hexagonal rings that expand when freezing.",
          guidedQuestion = "Did you think solids must always be denser, or were you focusing on total weight?",
          suggestedAnswers = listOf(
            "I assumed all solids must be denser than liquids",
            "I was focusing on raw weight instead of density",
            "Explain with an example"
          )
        )
      } else {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "🔍 **Diagnosing Misconceptions in $topic**:\n\n" +
              "In ${subject.displayName}, common mistakes usually come from:\n" +
              "• Conflating a rate of change with a total accumulated amount.\n" +
              "• Assuming a linear relationship when the system is quadratic or exponential.\n" +
              "• Overlooking a hidden boundary condition.",
          guidedQuestion = "Looking at your current understanding, which factor might need a slight adjustment?",
          suggestedAnswers = listOf(
            "Guide me step by step",
            "Explain with an example",
            "Make it easier"
          )
        )
      }
    }

    if (isExplainExample) {
      return if (isPython) {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "🌟 **Real-World Analogy & Code Example**:\n\n" +
              "Think of a function like a **Kitchen Smoothie Blender**:\n\n" +
              "```python\n" +
              "def make_smoothie(fruit, liquid):\n" +
              "    drink = f'Refreshing {fruit} smoothie with {liquid}'\n" +
              "    return drink\n" +
              "\n" +
              "# Using the function:\n" +
              "my_order = make_smoothie('Mango', 'Almond Milk')\n" +
              "print(my_order)\n" +
              "```\n" +
              "• **Inputs (Parameters)**: `'Mango'`, `'Almond Milk'`\n" +
              "• **Internal Work**: Blending ingredients together\n" +
              "• **Output (Return Value)**: The finished smoothie cup\n\n" +
              "You only assemble the blender once, but you can make 1,000 different drinks just by changing inputs!",
          guidedQuestion = "What would `make_smoothie('Strawberry', 'Yogurt')` return?",
          suggestedAnswers = listOf(
            "'Refreshing Strawberry smoothie with Yogurt'",
            "Why is return better than just print?",
            "Challenge me"
          )
        )
      } else if (isIce) {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "🌟 **Real-World Practical Example**:\n\n" +
              "Think about an **Ice Cube Tray in your Freezer**:\n\n" +
              "1. If you fill every compartment right up to the level edge with water and close the door, when you check it tomorrow, the ice cubes have bulged *above* the plastic rim!\n" +
              "2. The exact same number of water molecules now occupies ~9% more space.\n" +
              "3. Because that same weight is spread over a bigger volume, 1 cm³ of ice weighs ~0.92 grams, while 1 cm³ of water weighs 1.00 gram.\n" +
              "4. When placed in water, ice floats with ~10% of its volume poking above the waterline—just like an iceberg in the ocean!",
          guidedQuestion = "If you dropped that ice cube into cooking oil (density ~0.92 g/cm³), what do you think would happen?",
          suggestedAnswers = listOf(
            "It would hover in the middle or sink because densities are nearly equal",
            "Why do icebergs have 90% of their mass underwater?",
            "Challenge me"
          )
        )
      } else {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "🌟 **Practical Real-World Example for $topic**:\n\n" +
              "Consider a thermostat in a room or an automated checkout counter. When conditions shift, the underlying rules in ${subject.displayName} guarantee predictable, consistent transformation.",
          guidedQuestion = "How does this practical scenario help you picture the underlying mechanics?",
          suggestedAnswers = listOf(
            "It makes the concept concrete and easy to visualize",
            "Show me a step-by-step example",
            "Give me the answer"
          )
        )
      }
    }

    if (isMakeEasier) {
      return if (isPython) {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "✨ **Simplified in Plain Words**:\n\nA function is simply a **recipe with a name**.\nWhenever you want to bake the cake or calculate the total, you just call its name instead of rewriting 20 lines of instructions every single time.",
          guidedQuestion = "If a recipe needs 'sugar' and 'flour' to work, what do we call those ingredients in a function?",
          suggestedAnswers = listOf(
            "Parameters or input arguments",
            "Explain with an example",
            "Give me the answer"
          )
        )
      } else if (isIce) {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "✨ **Simplified in Plain Words**:\n\nWhen water freezes into ice, it expands and gets **fluffier and more spread out**.\nBecause it takes up more space without gaining any extra weight, it is lighter for its size than liquid water—so it floats to the top!",
          guidedQuestion = "Just like a hollow beach ball floats because it is fluffy and light for its size, does ice float for the same reason?",
          suggestedAnswers = listOf(
            "Yes! Greater volume makes it lighter per unit of space",
            "Explain with an example",
            "Give me the answer"
          )
        )
      } else {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "✨ **Simplified in Plain Words for $topic**:\n\nAt its core in ${subject.displayName}, this concept is about one simple rule: when you change input A, output B responds in a direct, predictable way.",
          guidedQuestion = "If you imagine the simplest possible case, what is the single main thing that happens?",
          suggestedAnswers = listOf(
            "Explain with an example",
            "Guide me step by step",
            "Give me the answer"
          )
        )
      }
    }

    if (isChallengeMe) {
      return if (isPython) {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "⚡ **Challenge Scenario: The Mutable Default Quirk**:\n\nLook at this Python function:\n```python\ndef append_item(val, target_list=[]):\n    target_list.append(val)\n    return target_list\n\nprint(append_item(1))\nprint(append_item(2))\n```",
          guidedQuestion = "Why does the second call output `[1, 2]` instead of `[2]`, and how do professional Python developers fix this bug?",
          suggestedAnswers = listOf(
            "Default lists are created once at definition time; fix using target_list=None",
            "It creates a new empty list on each call",
            "How do we write safe default arguments?"
          )
        )
      } else if (isIce) {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "⚡ **Challenge Scenario: The Floating Ice Melting Paradox**:\n\nA glass of water is filled to the absolute brim with an ice cube floating in it.",
          guidedQuestion = "When the ice cube melts completely into liquid water, will the water spill over the brim, drop slightly, or remain exactly at the brim? Why?",
          suggestedAnswers = listOf(
            "It remains exactly at the brim because displaced mass equals melted water mass",
            "It will overflow because ice expands",
            "The water level drops slightly"
          )
        )
      } else {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "⚡ **Deep Challenge Problem in ${subject.displayName}**:\n\nConsider an extreme edge-case for '$topic' where one boundary parameter is set to zero and another approaches infinity.",
          guidedQuestion = "How does the system behave at this critical asymptotic boundary?",
          suggestedAnswers = listOf(
            "It stabilizes at a critical threshold",
            "Let's work through the proof",
            "Give me a hint"
          )
        )
      }
    }

    if (isGiveAnswer) {
      return if (isPython) {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "🎯 **Complete Answer & Comprehensive Explanation**:\n\n" +
              "In Python, a **function** is a reusable, named block of code that isolates and executes a specific task.\n\n" +
              "**Key Mechanics**:\n" +
              "1. **Definition**: Defined with `def function_name(param1, param2):`\n" +
              "2. **Parameters**: Allow functions to receive inputs dynamically.\n" +
              "3. **Return Value**: Using `return`, the function hands processed results back to the caller.\n" +
              "4. **Encapsulation & DRY**: Prevents code duplication (Don't Repeat Yourself) and scopes variables locally.\n\n" +
              "```python\n" +
              "def calculate_tax(price, rate=0.08):\n" +
              "    return round(price * rate, 2)\n" +
              "\n" +
              "tax = calculate_tax(100.0) # Returns 8.0\n" +
              "```",
          guidedQuestion = "You now have the full picture! Would you like to tackle a challenge problem or explore another concept?",
          suggestedAnswers = listOf(
            "Challenge me with a Python quiz question",
            "Explain function scope and closures",
            "Let's explore another topic"
          ),
          isKeyInsight = true
        )
      } else if (isIce) {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "🎯 **Complete Answer & Comprehensive Explanation**:\n\n" +
              "**Ice floats on water because solid ice is ~9% less dense than liquid water.**\n\n" +
              "**The Full Physics & Chemistry Breakdown**:\n" +
              "1. **Molecular Geometry**: Liquid water molecules are loosely packed and slide past each other. When water cools below 4°C, hydrogen bonds force molecules into a fixed hexagonal crystal lattice.\n" +
              "2. **Volume Expansion**: The hexagonal lattice contains open empty space between molecules, expanding the volume by ~9% without changing mass.\n" +
              "3. **Density Drop**: Density = Mass / Volume. Because volume increases, the density of ice drops to $\\approx 0.917\\text{ g/cm}^3$, lower than liquid water ($1.00\\text{ g/cm}^3$ at 4°C).\n" +
              "4. **Buoyancy**: By Archimedes' Principle, the upward buoyant force equals the weight of displaced water, allowing ice to float with ~10% exposed above the surface.\n" +
              "5. **Ecological Impact**: This creates an insulating ice layer on lake surfaces, protecting aquatic life from freezing solid in winter.",
          guidedQuestion = "You have mastered the complete scientific principle! Would you like to try a challenge puzzle on Archimedes' Principle or explore another topic?",
          suggestedAnswers = listOf(
            "Challenge me on Archimedes' Principle",
            "Why does ice insulate the water beneath?",
            "Let's explore another topic"
          ),
          isKeyInsight = true
        )
      } else {
        ChatMessage(
          sender = MessageSender.TUTOR,
          text = "🎯 **Complete Answer & Solution for $topic**:\n\n" +
              "The foundational principle in ${subject.displayName} states that the outcome is determined by the fundamental laws of conservation and governing constraints of the system. Every input undergoes a defined mathematical/logical transformation to yield the final stable state.",
          guidedQuestion = "Now that you have the complete answer, would you like to test your mastery with a challenge question?",
          suggestedAnswers = listOf(
            "Challenge me",
            "Explain with an example",
            "Let's explore a new topic"
          ),
          isKeyInsight = true
        )
      }
    }

    val isStruggling = inputLower.contains("not sure") ||
        inputLower.contains("don't know") ||
        inputLower.contains("dont know") ||
        inputLower.contains("confus") ||
        inputLower.contains("simpler") ||
        inputLower.contains("help") ||
        inputLower.contains("what do you mean") ||
        inputLower.contains("another hint")

    when (learningMode) {
      LearningMode.HINT -> {
        if (topicLower.contains("python") || topicLower.contains("function")) {
          if (turn == 1 || inputLower.contains("hint")) {
            return ChatMessage(
              sender = MessageSender.TUTOR,
              text = "💡 Hint 2 (Structure): In Python, we define a function using the keyword `def`, followed by a name, parentheses for parameters `(param1, param2)`, and an indented body.",
              guidedQuestion = "If you want a function to return a calculated answer back to the caller instead of just printing it, what keyword do you use?",
              suggestedAnswers = listOf(
                "The 'return' keyword",
                "Give me Hint #3",
                "What is the difference between return and print?"
              )
            )
          } else {
            return ChatMessage(
              sender = MessageSender.TUTOR,
              text = "💡 Hint 3 (Execution): Exactly! `return` passes data back out. When called, a function executes its block and hands control back with the returned value.",
              guidedQuestion = "Now put it all together: how would you call a function named `calculate_area` that takes `width` and `height`?",
              suggestedAnswers = listOf(
                "area = calculate_area(5, 10)",
                "print calculate_area 5 10",
                "Show me a complete function example"
              ),
              isKeyInsight = true
            )
          }
        } else {
          return if (turn == 1 || inputLower.contains("hint")) {
            ChatMessage(
              sender = MessageSender.TUTOR,
              text = "💡 Hint 2 (Deeper Mechanism): Notice how the governing property changes when the variables interact. You noted: '$studentInput'.",
              guidedQuestion = "If the volume increases while mass stays constant, how does the resulting density compare to the surroundings?",
              suggestedAnswers = listOf(
                "The density decreases proportionally",
                "Give me another hint",
                "Can you show the mathematical formula?"
              )
            )
          } else {
            ChatMessage(
              sender = MessageSender.TUTOR,
              text = "💡 Final Hint & Solution Path: Since density = mass / volume, expanded volume means lower density, creating positive buoyancy!",
              guidedQuestion = "Can you summarize the complete deduction in your own words?",
              suggestedAnswers = listOf(
                "Ice expands upon freezing -> lower density -> it floats",
                "Let's try another topic",
                "How does this apply to other liquids?"
              ),
              isKeyInsight = true
            )
          }
        }
      }

      LearningMode.EXPLAIN -> {
        return ChatMessage(
          sender = MessageSender.TUTOR,
          text = "Great follow-up! You noted: '$studentInput'. To explain this deeper: when systems are modularized, each component encapsulates its internal state, exposing only clear interfaces.",
          guidedQuestion = "Does this step-by-step explanation make intuitive sense, or would you like to see a practical demonstration?",
          suggestedAnswers = listOf(
            "Yes, it makes complete sense!",
            "Show me a practical code demonstration",
            "What is an edge case where this breaks down?"
          ),
          isKeyInsight = turn >= 2
        )
      }

      LearningMode.CHALLENGE -> {
        if (inputLower.contains("unchanged") || inputLower.contains("displaced")) {
          return ChatMessage(
            sender = MessageSender.TUTOR,
            text = "Masterful critical thinking! Archimedes' Principle states that a floating body displaces its own weight in water. When the ice melts, its mass is identical, exactly filling the displaced volume without spilling a drop.",
            guidedQuestion = "What would happen if the ice cube contained a dense lead weight frozen inside it? Would the water level rise, fall, or stay level when it melts?",
            suggestedAnswers = listOf(
              "The water level will fall because submerged lead displaces only its volume",
              "The water level will rise",
              "It will stay exactly the same"
            ),
            isKeyInsight = true
          )
        }

        return ChatMessage(
          sender = MessageSender.TUTOR,
          text = "Intriguing hypothesis! You reasoned: '$studentInput'. Let's stress-test that logic.",
          guidedQuestion = "If we introduce an external perturbation or extreme temperature gradient, does your conclusion still hold, or does a new variable dominate?",
          suggestedAnswers = listOf(
            "A new variable like thermal expansion dominates",
            "The fundamental conservation law still holds",
            "Let's analyze the exact mathematical limits"
          ),
          isKeyInsight = turn >= 2
        )
      }

      LearningMode.SOCRATIC -> {
        // Misconception handling & Gentle scaffolding
        if (topicLower.contains("python") || topicLower.contains("function")) {
          if (inputLower.contains("smaller in size") || inputLower.contains("file size")) {
            return ChatMessage(
              sender = MessageSender.TUTOR,
              text = "While saving disk space is a minor benefit, think about what happens when you need to fix a bug in a calculation that appears 20 times across a program.",
              guidedQuestion = "If the calculation is defined in one central place instead of duplicated everywhere, how does that protect you from making mistakes when updating it?",
              suggestedAnswers = listOf(
                "You only need to update it once, ensuring consistency",
                "It reduces the memory needed to run",
                "Can you explain with a real example?"
              )
            )
          }

          if (isStruggling) {
            return ChatMessage(
              sender = MessageSender.TUTOR,
              text = "No problem at all! Let's use an everyday analogy. Imagine a blender in a kitchen. You don't rebuild the blender every time you want a smoothie.",
              guidedQuestion = "In programming, if a function is like that blender, what would you call the fruit you put into it, and what comes out?",
              suggestedAnswers = listOf(
                "Fruit is the input (arguments), smoothie is the output (return value)",
                "Fruit is a variable, smoothie is a print statement",
                "Let's see how this looks in code"
              )
            )
          }

          return when (turn) {
            1 -> ChatMessage(
              sender = MessageSender.TUTOR,
              text = "Spot on! Reusability and maintainability are the primary reasons. You accurately recognized: '$studentInput'.",
              guidedQuestion = "Now consider how a function communicates with the rest of the program: what does it accept as input, and how does it deliver results back?",
              suggestedAnswers = listOf(
                "It accepts parameters as input and returns a value as output",
                "It takes print statements and sends them to the screen",
                "How does a function know what variables to use?"
              )
            )
            2 -> ChatMessage(
              sender = MessageSender.TUTOR,
              text = "Precisely. Arguments provide the inputs, and the return statement outputs the calculated value.",
              guidedQuestion = "What would happen if a function tried to modify variables outside its own scope without returning them?",
              suggestedAnswers = listOf(
                "It causes unexpected side effects and makes debugging difficult",
                "The program runs much faster",
                "What is scope in Python?"
              )
            )
            else -> ChatMessage(
              sender = MessageSender.TUTOR,
              text = "Brilliant deduction! You have demonstrated a clear mental model of function encapsulation, parameters, and return values.",
              guidedQuestion = "Would you like to step up to a challenge on how pure functions and default arguments behave in Python?",
              suggestedAnswers = listOf(
                "Yes, give me a challenge scenario!",
                "How do default mutable arguments work?",
                "Let's explore another programming principle"
              ),
              isKeyInsight = true
            )
          }
        }

        if (topicLower.contains("ice") || topicLower.contains("float") || topicLower.contains("density")) {
          if (inputLower.contains("total weight") || inputLower.contains("heav") || inputLower.contains("regardless of volume")) {
            return ChatMessage(
              sender = MessageSender.TUTOR,
              text = "Notice that a massive steel cruise ship weighing 100,000 tons floats, while a tiny 5-gram pebble sinks immediately.",
              guidedQuestion = "Why do you think the heavy ship floats while the light pebble sinks? What role does the amount of water displaced (volume) play?",
              suggestedAnswers = listOf(
                "The ship's hollow shape gives it a lower average density than water",
                "Water pushes up harder on heavier objects",
                "Can you explain density with a simple formula?"
              )
            )
          }

          if (isStruggling) {
            return ChatMessage(
              sender = MessageSender.TUTOR,
              text = "Let's simplify! Density is simply how tightly packed matter is: mass divided by volume.",
              guidedQuestion = "If water expands into a hexagonal crystalline lattice when freezing—taking up MORE space for the same mass—does ice become more dense or less dense than liquid water?",
              suggestedAnswers = listOf(
                "Less dense, because the same mass occupies more volume",
                "More dense, because it became solid",
                "Why does water expand when freezing?"
              )
            )
          }

          return when (turn) {
            1 -> ChatMessage(
              sender = MessageSender.TUTOR,
              text = "Exact deduction! Density (mass/volume) relative to the surrounding liquid determines buoyancy.",
              guidedQuestion = "Water has a unique property: when it freezes from liquid to ice, it expands. What happens to its density during this expansion?",
              suggestedAnswers = listOf(
                "Its density decreases, making ice lighter than liquid water per unit volume",
                "Its density stays the same",
                "Why does ice expand when most solids contract?"
              )
            )
            2 -> ChatMessage(
              sender = MessageSender.TUTOR,
              text = "Outstanding! Because liquid water contracts as it cools until 4°C, then expands upon freezing, ice is ~9% less dense than liquid water.",
              guidedQuestion = "Why is this anomalous expansion of water crucial for aquatic life during freezing winters?",
              suggestedAnswers = listOf(
                "Ice forms on top as an insulating layer, keeping liquid water beneath",
                "It makes the water freeze from the bottom up",
                "How does the ice act as an insulator?"
              ),
              isKeyInsight = true
            )
            else -> ChatMessage(
              sender = MessageSender.TUTOR,
              text = "Phenomenal reasoning! You've traced the atomic structure of hydrogen bonds to ecological preservation.",
              guidedQuestion = "Would you like to test your understanding against a challenge on what happens to water levels when floating ice melts?",
              suggestedAnswers = listOf(
                "Yes, test me on melting ice water levels",
                "Explain hydrogen bonding geometry",
                "Explore another chemistry phenomenon"
              )
            )
          }
        }

        if (isStruggling) {
          return ChatMessage(
            sender = MessageSender.TUTOR,
            text = "Let's take a step back and look at the simplest case together. There are no wrong turns in inquiry.",
            guidedQuestion = "If we isolate just the core cause and effect: what is the single most noticeable change you observe when you test this in practice?",
            suggestedAnswers = listOf(
              "It causes an immediate change in the output",
              "Nothing changes until an external force acts",
              "Can you guide me with an everyday example?"
            )
          )
        }

        if (turn <= 1) {
          return ChatMessage(
            sender = MessageSender.TUTOR,
            text = "Good observation. You noted: '$studentInput'. Let's probe deeper into what causes that behavior.",
            guidedQuestion = "What underlying mechanism or rule do you think governs why that happens?",
            suggestedAnswers = listOf(
              "A fundamental conservation law or balance of forces",
              "An iterative division of the search space",
              "Let's test this with a specific case"
            )
          )
        } else if (turn == 2) {
          return ChatMessage(
            sender = MessageSender.TUTOR,
            text = "You are connecting the cause directly to the effect with sharp clarity.",
            guidedQuestion = "If you had to state this as a general rule that works every single time, how would you phrase it?",
            suggestedAnswers = listOf(
              "The outcome is always proportional to the governing ratio",
              "The system will naturally stabilize at equilibrium",
              "Can we verify this with an edge case?"
            ),
            isKeyInsight = true
          )
        } else {
          return ChatMessage(
            sender = MessageSender.TUTOR,
            text = "Superb Socratic synthesis! You have derived the foundational insight of '$topic' through your own reasoning.",
            guidedQuestion = "How would you apply this principle to solve a new, more complex challenge in ${subject.displayName}?",
            suggestedAnswers = listOf(
              "Give me an advanced challenge problem",
              "Connect this to a related concept",
              "Review the core steps we deduced"
            )
          )
        }
      }
    }
  }
}
