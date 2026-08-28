package com.example.quiz

import android.util.Log
import com.example.BuildConfig
import com.example.model.*
import com.example.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class QuizRepository {

  companion object {
    private const val TAG = "QuizRepository"
  }

  suspend fun generateQuiz(
    subject: Subject,
    topic: String,
    difficulty: Difficulty,
    history: List<ChatMessage>? = null,
    includeConversation: Boolean = false
  ): Quiz = withContext(Dispatchers.IO) {
    val cleanTopic = topic.trim().ifBlank { subject.sampleQuestions.first() }
    val apiKey = getApiKey()

    if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
      try {
        val geminiQuiz = fetchQuizFromGemini(
          apiKey = apiKey,
          subject = subject,
          topic = cleanTopic,
          difficulty = difficulty,
          history = history,
          includeConversation = includeConversation
        )
        if (geminiQuiz != null && geminiQuiz.questions.isNotEmpty()) {
          return@withContext geminiQuiz
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to generate quiz from Gemini: ${e.message}", e)
      }
    }

    // Heuristic generator fallback
    buildHeuristicQuiz(
      subject = subject,
      topic = cleanTopic,
      difficulty = difficulty,
      history = history,
      includeConversation = includeConversation
    )
  }

  private suspend fun fetchQuizFromGemini(
    apiKey: String,
    subject: Subject,
    topic: String,
    difficulty: Difficulty,
    history: List<ChatMessage>?,
    includeConversation: Boolean
  ): Quiz? {
    val conversationContext = if (includeConversation && !history.isNullOrEmpty()) {
      val studentMessages = history.filter { it.sender == MessageSender.STUDENT }.map { it.text }
      val tutorKeyPoints = history.filter { it.sender == MessageSender.TUTOR }.map { it.text.take(150) }
      """
      PREVIOUS LEARNING DIALOGUE CONTEXT:
      Student Questions/Responses: ${studentMessages.takeLast(4).joinToString(" | ")}
      Tutor Points Discussed: ${tutorKeyPoints.takeLast(3).joinToString(" | ")}
      Please tailor the questions specifically to test concepts touched upon in this learning dialogue.
      """.trimIndent()
    } else {
      ""
    }

    val prompt = """
      You are an expert pedagogical quiz generator. Create a high-quality, comprehensive 4-question diagnostic quiz on the topic: "$topic" in ${subject.displayName} at a ${difficulty.displayName} level.
      $conversationContext

      You MUST generate EXACTLY 4 questions, one of each specific type:
      1. MULTIPLE_CHOICE (type: "MULTIPLE_CHOICE", 4 distinct options with one unambiguously correct optionIndex 0-3)
      2. SHORT_ANSWER (type: "SHORT_ANSWER", precise single term, formula, or short phrase answer with rubric keywords)
      3. PROBLEM_SOLVING (type: "PROBLEM_SOLVING", calculation, code tracing, scenario deduction, or applied problem)
      4. CONCEPTUAL (type: "CONCEPTUAL", deep "why" / "how" probing foundational intuition and underlying principles)

      Respond STRICTLY with a valid JSON array containing 4 objects in the following format:
      [
        {
          "type": "MULTIPLE_CHOICE",
          "prompt": "Question text here",
          "context": "Brief context or hint setup",
          "options": ["Option A", "Option B", "Option C", "Option D"],
          "correctOptionIndex": 1,
          "correctAnswerText": "The exact correct option text",
          "acceptableKeywords": ["keyword1", "keyword2"],
          "conceptTag": "Specific Core Concept Name",
          "explanation": "Clear, step-by-step reasoning explaining why this is correct",
          "improvementTip": "Actionable advice on how to master this concept if missed"
        },
        {
          "type": "SHORT_ANSWER",
          "prompt": "Question text here",
          "context": "Context or setup",
          "options": [],
          "correctOptionIndex": null,
          "correctAnswerText": "Canonical short answer",
          "acceptableKeywords": ["keyword1", "keyword2", "keyword3"],
          "conceptTag": "Specific Core Concept Name",
          "explanation": "Detailed explanation",
          "improvementTip": "Improvement guidance"
        },
        {
          "type": "PROBLEM_SOLVING",
          "prompt": "Problem statement with specific values or code",
          "context": "Given constraints or equations",
          "options": [],
          "correctOptionIndex": null,
          "correctAnswerText": "Solved result and concise explanation",
          "acceptableKeywords": ["keyword1", "key_number"],
          "conceptTag": "Specific Applied Concept Name",
          "explanation": "Step-by-step solution derivation",
          "improvementTip": "Improvement guidance"
        },
        {
          "type": "CONCEPTUAL",
          "prompt": "Deep conceptual inquiry",
          "context": "Context",
          "options": [],
          "correctOptionIndex": null,
          "correctAnswerText": "First-principles explanation",
          "acceptableKeywords": ["concept1", "concept2"],
          "conceptTag": "Foundational Intuition Name",
          "explanation": "Thorough intuitive explanation",
          "improvementTip": "Improvement guidance"
        }
      ]
    """.trimIndent()

    val request = GeminiRequest(
      contents = listOf(
        GeminiContent(
          role = "user",
          parts = listOf(GeminiPart(text = prompt))
        )
      ),
      generationConfig = GeminiGenerationConfig(
        temperature = 0.5f,
        topP = 0.95f,
        maxOutputTokens = 2048
      )
    )

    val response = GeminiClient.api.generateContent(apiKey, request)
    val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
    if (responseText.isNullOrBlank()) return null

    return parseGeminiQuizJson(responseText, subject, topic, difficulty, includeConversation)
  }

  private fun parseGeminiQuizJson(
    jsonText: String,
    subject: Subject,
    topic: String,
    difficulty: Difficulty,
    basedOnConversation: Boolean
  ): Quiz? {
    try {
      val cleanJson = jsonText
        .trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()

      val jsonArray = if (cleanJson.startsWith("[")) {
        JSONArray(cleanJson)
      } else {
        val startIndex = cleanJson.indexOf('[')
        val endIndex = cleanJson.lastIndexOf(']')
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
          JSONArray(cleanJson.substring(startIndex, endIndex + 1))
        } else {
          return null
        }
      }

      val questions = mutableListOf<QuizQuestion>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        val typeStr = obj.optString("type", "MULTIPLE_CHOICE")
        val type = when (typeStr.uppercase()) {
          "MULTIPLE_CHOICE" -> QuestionType.MULTIPLE_CHOICE
          "SHORT_ANSWER" -> QuestionType.SHORT_ANSWER
          "PROBLEM_SOLVING" -> QuestionType.PROBLEM_SOLVING
          "CONCEPTUAL" -> QuestionType.CONCEPTUAL
          else -> QuestionType.MULTIPLE_CHOICE
        }

        val prompt = obj.getString("prompt")
        val context = obj.optString("context", "")
        val options = mutableListOf<String>()
        val optArray = obj.optJSONArray("options")
        if (optArray != null) {
          for (j in 0 until optArray.length()) {
            options.add(optArray.getString(j))
          }
        }

        val correctOptionIndex = if (obj.has("correctOptionIndex") && !obj.isNull("correctOptionIndex")) {
          obj.getInt("correctOptionIndex")
        } else null

        val correctAnswerText = obj.optString("correctAnswerText", "")
        val acceptableKeywords = mutableListOf<String>()
        val kwArray = obj.optJSONArray("acceptableKeywords")
        if (kwArray != null) {
          for (k in 0 until kwArray.length()) {
            acceptableKeywords.add(kwArray.getString(k))
          }
        }

        val conceptTag = obj.optString("conceptTag", "${subject.displayName} Core Principle")
        val explanation = obj.optString("explanation", "Understanding foundational rules in ${subject.displayName}.")
        val improvementTip = obj.optString("improvementTip", "Review the step-by-step principles of this concept with the AI Tutor.")

        questions.add(
          QuizQuestion(
            id = UUID.randomUUID().toString(),
            type = type,
            prompt = prompt,
            context = context,
            options = options,
            correctOptionIndex = correctOptionIndex,
            correctAnswerText = correctAnswerText,
            acceptableKeywords = acceptableKeywords,
            conceptTag = conceptTag,
            explanation = explanation,
            improvementTip = improvementTip
          )
        )
      }

      if (questions.isNotEmpty()) {
        return Quiz(
          id = UUID.randomUUID().toString(),
          subject = subject,
          topic = topic,
          difficulty = difficulty,
          basedOnConversation = basedOnConversation,
          questions = questions
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing Gemini quiz JSON: ${e.message}", e)
    }
    return null
  }

  fun evaluateQuiz(
    quiz: Quiz,
    userAnswers: Map<String, String>
  ): QuizEvaluation {
    val results = mutableListOf<QuestionResult>()
    val conceptsToImprove = mutableListOf<ConceptImprovementArea>()

    for (question in quiz.questions) {
      val userAns = userAnswers[question.id]?.trim() ?: ""

      val isCorrect: Boolean = when (question.type) {
        QuestionType.MULTIPLE_CHOICE -> {
          val selectedIdx = userAns.toIntOrNull()
          if (selectedIdx != null && question.correctOptionIndex != null) {
            selectedIdx == question.correctOptionIndex
          } else if (userAns.isNotBlank() && question.correctAnswerText.isNotBlank()) {
            userAns.equals(question.correctAnswerText, ignoreCase = true) ||
                (question.correctOptionIndex != null && question.options.getOrNull(question.correctOptionIndex)?.equals(userAns, ignoreCase = true) == true)
          } else {
            false
          }
        }
        QuestionType.SHORT_ANSWER,
        QuestionType.PROBLEM_SOLVING,
        QuestionType.CONCEPTUAL -> {
          if (userAns.isBlank()) {
            false
          } else {
            val normalizedUser = userAns.lowercase()
            val normalizedCorrect = question.correctAnswerText.lowercase()

            val directMatch = normalizedUser.contains(normalizedCorrect) || normalizedCorrect.contains(normalizedUser)
            val keywordMatch = question.acceptableKeywords.any { kw ->
              normalizedUser.contains(kw.lowercase())
            }

            directMatch || keywordMatch
          }
        }
      }

      val feedback = if (isCorrect) {
        "Excellent! Your reasoning correctly captures the core mechanism of ${question.conceptTag}."
      } else {
        if (userAns.isBlank()) {
          "Question was left unanswered. Key concept: ${question.conceptTag}."
        } else {
          "Your answer did not match the expected core concept: ${question.conceptTag}. See the explanation below to reinforce your understanding."
        }
      }

      results.add(
        QuestionResult(
          question = question,
          userResponse = userAns,
          isCorrect = isCorrect,
          feedbackNote = feedback
        )
      )

      if (!isCorrect) {
        conceptsToImprove.add(
          ConceptImprovementArea(
            conceptName = question.conceptTag,
            subject = quiz.subject,
            description = question.improvementTip.ifBlank { "Strengthen fundamental understanding of ${question.conceptTag}." },
            remedialPrompt = "Can you help me understand and master the concept of '${question.conceptTag}' from my quiz on '${quiz.topic}'?"
          )
        )
      }
    }

    val score = results.count { it.isCorrect }
    val total = results.size
    val percentage = if (total > 0) (score * 100) / total else 0

    return QuizEvaluation(
      quiz = quiz,
      results = results,
      score = score,
      totalQuestions = total,
      percentage = percentage,
      conceptsToImprove = conceptsToImprove.distinctBy { it.conceptName }
    )
  }

  private fun buildHeuristicQuiz(
    subject: Subject,
    topic: String,
    difficulty: Difficulty,
    history: List<ChatMessage>?,
    includeConversation: Boolean
  ): Quiz {
    val topicLower = topic.lowercase()
    val isIce = topicLower.contains("ice") || topicLower.contains("density") || topicLower.contains("buoyancy") || topicLower.contains("float")
    val isPython = topicLower.contains("python") || topicLower.contains("function") || topicLower.contains("code") || topicLower.contains("programming")
    val isNegativeMath = topicLower.contains("negative") || topicLower.contains("multiply") || topicLower.contains("number line")

    val questions: List<QuizQuestion> = when {
      isIce -> getIceQuizQuestions(difficulty)
      isPython -> getPythonQuizQuestions(difficulty)
      isNegativeMath -> getNegativeMathQuizQuestions(difficulty)
      else -> getGenericSubjectQuizQuestions(subject, topic, difficulty)
    }

    return Quiz(
      id = UUID.randomUUID().toString(),
      subject = subject,
      topic = topic,
      difficulty = difficulty,
      basedOnConversation = includeConversation && !history.isNullOrEmpty(),
      questions = questions
    )
  }

  private fun getIceQuizQuestions(difficulty: Difficulty): List<QuizQuestion> {
    return listOf(
      QuizQuestion(
        id = "ice_mcq_1",
        type = QuestionType.MULTIPLE_CHOICE,
        prompt = "Why does solid ice float on top of liquid water, unlike most other substances?",
        context = "Consider the molecular arrangement when water freezes below 4°C.",
        options = listOf(
          "Ice contains trapped microscopic air pockets that pull it upward",
          "Hydrogen bonds form an open hexagonal lattice that expands volume, reducing density",
          "Cold water currents actively push the ice upward via convection",
          "Surface tension prevents solids from breaching the water boundary"
        ),
        correctOptionIndex = 1,
        correctAnswerText = "Hydrogen bonds form an open hexagonal lattice that expands volume, reducing density",
        acceptableKeywords = listOf("hexagonal", "lattice", "density", "volume", "hydrogen"),
        conceptTag = "Hydrogen Bonding & Density Anomaly",
        explanation = "As water cools below 4°C, hydrogen bonding locks molecules into an open hexagonal crystal lattice. This increases the volume by ~9% for the same mass, making ice less dense (~0.917 g/cm³) than liquid water (1.00 g/cm³).",
        improvementTip = "Review how crystalline geometry and intermolecular hydrogen bonds dictate density and volume changes."
      ),
      QuizQuestion(
        id = "ice_short_2",
        type = QuestionType.SHORT_ANSWER,
        prompt = "What physical principle dictates that the upward buoyant force on an object equals the weight of the fluid it displaces?",
        context = "Name the classical principle discovered in ancient Syracuse.",
        correctAnswerText = "Archimedes' Principle",
        acceptableKeywords = listOf("archimedes", "archimedes principle", "buoyancy principle", "archimedes' principle"),
        conceptTag = "Archimedes' Principle of Buoyancy",
        explanation = "Archimedes' Principle states that any body completely or partially submerged in a fluid is buoyed up by a force equal to the weight of the fluid displaced by the body.",
        improvementTip = "Focus on the relationship between fluid displacement, buoyant force, and net gravity balance."
      ),
      QuizQuestion(
        id = "ice_prob_3",
        type = QuestionType.PROBLEM_SOLVING,
        prompt = "If the density of ice is 0.92 g/cm³ and seawater density is 1.025 g/cm³, what approximate percentage of a floating iceberg's total volume remains submerged beneath the surface?",
        context = "Use the ratio: Submerged Fraction = Density(ice) / Density(seawater).",
        correctAnswerText = "Approximately 90% (89.7%)",
        acceptableKeywords = listOf("90", "89.7", "89.8", "90%", "89.7%"),
        conceptTag = "Buoyancy Ratio & Fluid Displacement",
        explanation = "Fraction submerged = Density(ice) / Density(fluid) = 0.92 / 1.025 ≈ 0.8975 or ~89.8%. Therefore, roughly 90% of an iceberg is underwater!",
        improvementTip = "Practice calculating buoyancy ratios by dividing the object density by the fluid medium density."
      ),
      QuizQuestion(
        id = "ice_concept_4",
        type = QuestionType.CONCEPTUAL,
        prompt = "How does the floating behavior of ice prevent lakes and oceans from freezing solid to the bottom during harsh winters?",
        context = "Think about thermal conductivity and ecological insulation.",
        correctAnswerText = "The floating top layer of ice acts as an insulating thermal barrier that slows heat loss from the warmer liquid water below.",
        acceptableKeywords = listOf("insulat", "barrier", "protect", "freeze from top", "traps heat", "thermal insulator", "bottom"),
        conceptTag = "Thermal Insulation & Aquatic Ecology",
        explanation = "Because ice floats, it forms a protective crust on top of lakes. Ice has low thermal conductivity, creating an insulating blanket that shields the liquid water below (at ~4°C) from subzero air temperatures, preserving aquatic life.",
        improvementTip = "Connect the anomalous expansion of water with ecological thermodynamics and heat transfer."
      )
    )
  }

  private fun getPythonQuizQuestions(difficulty: Difficulty): List<QuizQuestion> {
    return listOf(
      QuizQuestion(
        id = "py_mcq_1",
        type = QuestionType.MULTIPLE_CHOICE,
        prompt = "What is the primary difference between `print()` and `return` inside a Python function?",
        context = "Consider program data flow versus human terminal output.",
        options = listOf(
          "`print()` returns a string to callers, while `return` writes directly to console",
          "`print()` only displays output to the screen, while `return` passes data back to the caller",
          "`return` terminates the program entirely, while `print()` pauses execution",
          "There is no difference; both are interchangeable in modern Python"
        ),
        correctOptionIndex = 1,
        correctAnswerText = "`print()` only displays output to the screen, while `return` passes data back to the caller",
        acceptableKeywords = listOf("displays", "caller", "passes data", "return", "screen"),
        conceptTag = "Function Return Values vs Terminal Output",
        explanation = "`print()` is a side-effect function that outputs text to standard output for humans to read. `return` hands data directly back into program memory so subsequent variables or functions can process it.",
        improvementTip = "Ensure you distinguish between displaying values for debugging and returning usable data to the program."
      ),
      QuizQuestion(
        id = "py_short_2",
        type = QuestionType.SHORT_ANSWER,
        prompt = "What keyword is used in Python to define a new function?",
        context = "Three lowercase letters.",
        correctAnswerText = "def",
        acceptableKeywords = listOf("def", "def keyword", "`def`"),
        conceptTag = "Function Declaration Syntax",
        explanation = "The `def` keyword (short for 'define') introduces a function definition in Python, followed by the function name, parameter list in parentheses, and a colon.",
        improvementTip = "Practice the standard Python function signature: `def name(args):`."
      ),
      QuizQuestion(
        id = "py_prob_3",
        type = QuestionType.PROBLEM_SOLVING,
        prompt = "Trace this code. What will be the final value printed?\n\n```python\ndef multiplier(x, y=3):\n    return x * y\n\nval = multiplier(4)\nval = multiplier(val, 2)\nprint(val)\n```",
        context = "Evaluate the default parameter on the first call, then pass the result into the second call.",
        correctAnswerText = "24",
        acceptableKeywords = listOf("24", "twenty four", "24.0"),
        conceptTag = "Default Arguments & Function Composition",
        explanation = "First call: `multiplier(4)` uses default `y=3`, returning `4 * 3 = 12`. Second call: `multiplier(12, 2)` overrides `y` with `2`, returning `12 * 2 = 24`.",
        improvementTip = "Carefully trace variable states step-by-step through successive function invocations."
      ),
      QuizQuestion(
        id = "py_concept_4",
        type = QuestionType.CONCEPTUAL,
        prompt = "Why are functions essential for adhering to the DRY (Don't Repeat Yourself) principle and managing variable scope in software engineering?",
        context = "Explain code reusability, modularity, and encapsulation.",
        correctAnswerText = "Functions encapsulate reusable logic into isolated blocks with local scope, eliminating redundant copy-pasted code and preventing global variable collisions.",
        acceptableKeywords = listOf("reus", "encapsulat", "scope", "modul", "duplicate", "redundant", "dry", "isolat"),
        conceptTag = "DRY Principle & Variable Encapsulation",
        explanation = "Functions let developers write a solution once and reuse it across multiple contexts. They also create a local variable namespace, preventing unintended side effects or variable contamination in global state.",
        improvementTip = "Study how modularity and localized variable scope make code maintainable and testable."
      )
    )
  }

  private fun getNegativeMathQuizQuestions(difficulty: Difficulty): List<QuizQuestion> {
    return listOf(
      QuizQuestion(
        id = "math_mcq_1",
        type = QuestionType.MULTIPLE_CHOICE,
        prompt = "On a standard number line, how can multiplying by a negative number (-1) be geometrically interpreted?",
        context = "Think about orientation and rotation from the origin.",
        options = listOf(
          "Shifting the point 1 unit to the left",
          "A 180-degree reversal of direction (reflection across the origin)",
          "Scaling the distance from origin to infinity",
          "Dividing the coordinate by 2"
        ),
        correctOptionIndex = 1,
        correctAnswerText = "A 180-degree reversal of direction (reflection across the origin)",
        acceptableKeywords = listOf("180", "reversal", "reflection", "direction", "origin"),
        conceptTag = "Number Line Geometric Transformations",
        explanation = "Multiplying by -1 acts as a reflection or 180° rotation around zero. Doing it once flips positive to negative; doing it twice flips back from negative to positive!",
        improvementTip = "Visualize multiplying by negatives as reversing direction rather than an abstract arbitrary rule."
      ),
      QuizQuestion(
        id = "math_short_2",
        type = QuestionType.SHORT_ANSWER,
        prompt = "What mathematical property allows us to prove that (-a) × (-b) = a × b by expanding a × (b + (-b)) = 0?",
        context = "The property relating multiplication across addition.",
        correctAnswerText = "The Distributive Property",
        acceptableKeywords = listOf("distributive", "distributive property", "distributivity", "distributive law"),
        conceptTag = "Distributive Law of Arithmetic",
        explanation = "The distributive law x(y + z) = xy + xz is foundational to arithmetic consistency. For it to hold universally, negative times negative must yield positive.",
        improvementTip = "Review how fundamental algebraic field properties (distributivity and additive inverses) enforce sign rules."
      ),
      QuizQuestion(
        id = "math_prob_3",
        type = QuestionType.PROBLEM_SOLVING,
        prompt = "Evaluate the expression: (-3) × (-4) - (-5) × 2.",
        context = "Follow order of operations: perform multiplications first, then subtraction.",
        correctAnswerText = "22",
        acceptableKeywords = listOf("22", "twenty two", "22.0"),
        conceptTag = "Signed Integer Operations & Precedence",
        explanation = "Multiplication 1: (-3) × (-4) = +12. Multiplication 2: (-5) × 2 = -10. Expression becomes: 12 - (-10) = 12 + 10 = 22.",
        improvementTip = "Double check double negatives during subtraction: subtracting a negative is equivalent to adding a positive."
      ),
      QuizQuestion(
        id = "math_concept_4",
        type = QuestionType.CONCEPTUAL,
        prompt = "Why would arithmetic break down if negative times negative resulted in a negative number?",
        context = "Consider the distributive property and additive inverses.",
        correctAnswerText = "It would violate the distributive property, meaning a(b - b) = a(0) = 0 would no longer hold, causing mathematical contradictions.",
        acceptableKeywords = listOf("distributive", "contradiction", "break", "inconsistent", "zero", "identity", "inverse"),
        conceptTag = "Arithmetic Axioms & Structural Consistency",
        explanation = "If (-1) × (-1) = -1, then (-1)(1 + (-1)) = (-1)(1) + (-1)(-1) = -1 + (-1) = -2. But (-1)(0) = 0, so 0 = -2, a total contradiction!",
        improvementTip = "Study how mathematical consistency requires consistent axioms across all operations."
      )
    )
  }

  private fun getGenericSubjectQuizQuestions(
    subject: Subject,
    topic: String,
    difficulty: Difficulty
  ): List<QuizQuestion> {
    return listOf(
      QuizQuestion(
        id = "${subject.name.lowercase()}_mcq_1",
        type = QuestionType.MULTIPLE_CHOICE,
        prompt = "Which foundational principle best explains the primary mechanism behind '$topic' in ${subject.displayName}?",
        context = "Focus on the governing laws and causal relationships.",
        options = listOf(
          "Direct conservation of fundamental properties and state equilibria",
          "Random spontaneous fluctuations without underlying rules",
          "Arbitrary conventions without physical or logical constraints",
          "Unbounded exponential divergence without limiting factors"
        ),
        correctOptionIndex = 0,
        correctAnswerText = "Direct conservation of fundamental properties and state equilibria",
        acceptableKeywords = listOf("conservation", "equilibria", "mechanism", "fundamental", "governing"),
        conceptTag = "Foundational Mechanisms of ${subject.displayName}",
        explanation = "In ${subject.displayName}, systems are governed by conservation laws, logical consistency, and predictable relationships between input variables.",
        improvementTip = "Identify the core governing constraints that balance inputs and outputs."
      ),
      QuizQuestion(
        id = "${subject.name.lowercase()}_short_2",
        type = QuestionType.SHORT_ANSWER,
        prompt = "What is the primary governing factor or parameter that determines the rate of change or output for '$topic'?",
        context = "Consider the key variable in ${subject.displayName}.",
        correctAnswerText = "The input gradient or driving potential",
        acceptableKeywords = listOf("gradient", "potential", "input", "rate", "driving force", "variable", "factor", "constraint"),
        conceptTag = "System Variables & Governing Dynamics",
        explanation = "The driving potential or input gradient dictates how dynamic systems in ${subject.displayName} evolve over time.",
        improvementTip = "Focus on identifying independent versus dependent variables in the system."
      ),
      QuizQuestion(
        id = "${subject.name.lowercase()}_prob_3",
        type = QuestionType.PROBLEM_SOLVING,
        prompt = "If the primary input parameter for '$topic' is doubled while holding all other constraints constant, how does the resulting outcome behave in a linear system?",
        context = "Apply proportional scaling.",
        correctAnswerText = "The outcome doubles (scales by a factor of 2)",
        acceptableKeywords = listOf("double", "doubles", "2", "factor of 2", "twice", "2x"),
        conceptTag = "Proportional Scaling & Analytical Deduction",
        explanation = "In any direct linear relationship, doubling the driving variable proportionally doubles the output (y = kx implies 2y = k(2x)).",
        improvementTip = "Practice analyzing how system responses scale when individual parameters are varied."
      ),
      QuizQuestion(
        id = "${subject.name.lowercase()}_concept_4",
        type = QuestionType.CONCEPTUAL,
        prompt = "Why is understanding first-principles reasoning more effective than memorizing formulas when analyzing '$topic'?",
        context = "Explain intuition, edge cases, and problem transfer.",
        correctAnswerText = "First principles allow one to derive solutions for novel edge cases and adapt to new scenarios rather than relying on brittle memory.",
        acceptableKeywords = listOf("first principle", "intuition", "transfer", "adapt", "derive", "understand", "novel", "why"),
        conceptTag = "First-Principles Deduction in ${subject.displayName}",
        explanation = "First-principles deduction breaks down complex phenomena into undeniable foundational truths, enabling deep problem solving even when presented with unfamiliar constraints.",
        improvementTip = "Ask 'why' at each step of a derivation rather than merely memorizing formulas."
      )
    )
  }

  private fun getApiKey(): String {
    return try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Exception) {
      ""
    }
  }
}
