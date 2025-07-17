package chatbot.main

import scala.io.Source
import scala.util.{Try, Success, Failure}
import java.io.{File, PrintWriter, FileWriter}
import scala.collection.mutable
import scala.util.Random

object Constants {
  val CMD_UNKNOWN = "UNKNOWN"
  val CMD_HELP = "HELP"
  val CMD_LIST_PLANETS = "LIST_PLANETS"
  val CMD_LIST_CATEGORY = "LIST_CATEGORY"
  val CMD_RANDOM_FACT = "RANDOM_FACT"
  val CMD_START_QUIZ = "START_QUIZ"
  val CMD_ANSWER_QUIZ = "ANSWER_QUIZ"
  val CMD_ASK_ABOUT = "ASK_ABOUT"
  val CMD_COMPARE = "COMPARE"
  val CMD_EXIT_QUIZ = "EXIT_QUIZ"
  val CMD_SKIP_QUESTION = "SKIP_QUESTION"
  val CMD_GREETINGS = "GREETINGS"

  // Word lists
  val helpWords = List("help", "commands", "guide", "instructions")
  val listWords = List("list", "show", "display", "name", "what")
  val factWords = List(
    "fact", "facts", "trivia", "interesting", "random", "cool", "fun",
    "tell me a fact", "give me a fact", "share a fact", "tell me something",
    "surprise me", "did you know"
  )
  val quizWords = List("quiz", "trivia", "test", "challenge", "game")
  val compareWords = List(
    "compare", "difference", "versus", "vs", "between", "against", "and",
    "or", "difference between", "how does", "how do", "what is the difference"
  )
  val exitWords = List("exit", "quit", "stop", "end")
  val greetingWords = List("hello", "hi", "hey", "greetings", "yo")
  val planets = List("mars", "jupiter", "saturn", "uranus", "neptune", "venus", "mercury", "earth", "pluto")

  val categories = List(
    "stars", "constellations", "moons", "dwarf planets", "galaxies",
    "black holes", "asteroids", "comets", "nebulae", "star systems", "exoplanets"
  )

  // Add celestial object types
  val celestialObjects = List(
    "stars", "planets", "moons", "dwarf planets", "galaxies",
    "comets", "asteroids", "nebulae"
  )
}

// ANSI Color codes for terminal
object Colors {
  val Reset = "\u001B[0m"
  val Black = "\u001B[30m"
  val Red = "\u001B[31m"
  val Green = "\u001B[32m"
  val Yellow = "\u001B[33m"
  val Blue = "\u001B[34m"
  val Purple = "\u001B[35m"
  val Cyan = "\u001B[36m"
  val Blink = "\u001B[5m"
}
import Colors._

// Configuration class
case class Config()
object Config {
  def load: Config = Config()
}

// Type aliases to improve readability
type AstronomyData = Map[String, Map[String, String]]
type Analytics = AnalyticsImpl
type QuizManager = QuizManagerImpl

object DataLoader {
  def loadAstronomyData(filename: String): Map[String, Map[String, String]] = {
    import scala.io.Source

    try {
      val jsonContent = Source.fromFile(filename).mkString

      def parseObjects(json: String): List[Map[String, String]] = {
        val objectsStr = json.trim.stripPrefix("[").stripSuffix("]").trim
        val objectStrings = splitObjects(objectsStr)
        objectStrings.map(parseObject)
      }

      def splitObjects(str: String): List[String] = {
        var objects = List.empty[String]
        var currentObject = new StringBuilder
        var braceCount = 0
        var inQuotes = false

        str.foreach { char =>
          currentObject.append(char)
          char match {
            case '"' => inQuotes = !inQuotes
            case '{' if !inQuotes => braceCount += 1
            case '}' if !inQuotes =>
              braceCount -= 1
              if (braceCount == 0) {
                objects = objects :+ currentObject.toString.trim
                currentObject = new StringBuilder
              }
            case _ =>
          }
        }
        objects.filterNot(_.trim.isEmpty)
      }

      def parseObject(objectStr: String): Map[String, String] = {
        val cleanStr = objectStr.trim.stripPrefix("{").stripSuffix("}").trim
        val pairs = splitKeyValuePairs(cleanStr)

        pairs.map { pair =>
          val Array(key, value) = pair.split(":", 2)
          val cleanKey = key.trim.stripPrefix("\"").stripSuffix("\"")
          val cleanValue = value.trim.stripPrefix("\"").stripSuffix("\"").stripSuffix(",")
          cleanKey -> cleanValue
        }.toMap
      }

      def splitKeyValuePairs(str: String): List[String] = {
        var pairs = List.empty[String]
        var currentPair = new StringBuilder
        var inQuotes = false
        var depth = 0

        str.foreach {
          case '"' =>
            inQuotes = !inQuotes
            currentPair.append('"')
          case '{' | '[' if !inQuotes =>
            depth += 1
            currentPair.append('{')
          case '}' | ']' if !inQuotes =>
            depth -= 1
            currentPair.append('}')
          case ',' if !inQuotes && depth == 0 =>
            pairs = pairs :+ currentPair.toString.trim
            currentPair = new StringBuilder
          case c =>
            currentPair.append(c)
        }
        if (currentPair.nonEmpty) pairs = pairs :+ currentPair.toString.trim
        pairs.filterNot(_.trim.isEmpty)
      }

      val objects = parseObjects(jsonContent)
      objects.map { obj =>
        val name = obj.getOrElse("name", "unknown").toLowerCase
        name -> obj
      }.toMap

    } catch {
      case e: Exception =>
        println(s"Error loading astronomy data: ${e.getMessage}")
        Map.empty[String, Map[String, String]]
    }
  }
  
  def loadSpaceObjectsData(filename: String): Map[String, Map[String, String]] = {
    try {
      val source = Source.fromFile(filename)
      val lines = source.getLines().toList
      source.close()

      if (lines.isEmpty) return Map.empty

      val headers = lines.head.split(",").map(_.trim)
      val data = lines.tail.flatMap { line =>
        try {
          // Split by comma but respect quoted values
          val values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)").map { value =>
            // Remove quotes and trim
            value.trim.stripPrefix("\"").stripSuffix("\"").trim
          }
          
          if (values.length >= headers.length) {
            val objectData = headers.zip(values).toMap
            val name = objectData.getOrElse("name", "unknown").toLowerCase
            // Only include entries that have a name
            if (name != "unknown") Some(name -> objectData)
            else None
          } else None
        } catch {
          case e: Exception =>
            println(s"${Red}Warning: Skipping malformed line in CSV: $line${Reset}")
            None
        }
      }.toMap

      data
    } catch {
      case e: Exception =>
        println(s"${Red}Error loading space objects data: ${e.getMessage}${Reset}")
        Map.empty[String, Map[String, String]]
    }
  }
  
  // Merge data from both sources
  def mergeData(jsonData: Map[String, Map[String, String]], csvData: Map[String, Map[String, String]]): Map[String, Map[String, String]] = {
    val allKeys = jsonData.keySet ++ csvData.keySet
    allKeys.map { key =>
      key -> {
        val jsonEntry = jsonData.getOrElse(key, Map.empty)
        val csvEntry = csvData.getOrElse(key, Map.empty)
        // CSV data takes precedence over JSON data for the same fields
        jsonEntry ++ csvEntry
      }
    }.toMap
  }

  // This will be initialized when needed
  lazy val astronomyData: Map[String, Map[String, String]] = {
    val jsonData = loadAstronomyData("astronomy.json")
    val csvData = loadSpaceObjectsData("space_objects.csv")
    mergeData(jsonData, csvData)
  }
  lazy val spaceObjectsData: Map[String, Map[String, String]] = loadSpaceObjectsData("space_objects.csv")
}

object InputParser {
  import Constants._
  
  def parseInput(input: String, isQuizActive: Boolean = false): String = {
    val normalizedInput = input.trim.toLowerCase
    if (normalizedInput.isEmpty) return "unknown_empty"
    
    val words = normalizedInput.split("\\s+").toList
    
    // Handle quiz mode separately
    if (isQuizActive) {
      val (commandType, payload, extraPayload) = parseQuizMode(words, normalizedInput)
      return commandType.toLowerCase
    }
    
    // Check for random fact first
    if (normalizedInput == "random fact" || normalizedInput == "fact" || 
        (words.contains("random") && words.contains("fact"))) {
      return "randomfact"
    }
    
    // Check for comparison
    if (normalizedInput.contains("compare") || normalizedInput.contains("vs") || 
        normalizedInput.contains("versus")) {
      val objects = normalizedInput.split("(?:compare|vs|versus|and|between)").map(_.trim).filter(_.nonEmpty)
      if (objects.length >= 2) {
        return s"compare_${objects(0)}_${objects(1)}"
      }
    }
    
    // Rest of the command parsing
    val (commandType, payload, extraPayload) = parseRegularMode(words, normalizedInput)
    
    commandType.toLowerCase match {
      case "unknown" => "unknown_" + payload
      case "help" => "help"
      case "list_planets" => "listplanets"
      case "list_category" => "listcategory_" + payload
      case "random_fact" => "randomfact"
      case "start_quiz" => "startquiz"
      case "answer_quiz" => "answerquiz_" + payload
      case "ask_about" => "askabout_" + payload
      case "compare" => "compare_" + payload + "_" + extraPayload
      case "exit_quiz" => "exit"
      case "skip_question" => "startquiz"
      case "greetings" => "greetings"
      case _ => "unknown_" + normalizedInput
    }
  }

  def containsAny(words: List[String], targetList: List[String]): Boolean =
    words.exists(word => targetList.contains(word))

  def matchesGreetings(words: List[String]): Boolean =
    containsAny(words, greetingWords) || words.contains("chaturn")

  def matchesHelp(words: List[String]): Boolean =
    containsAny(words, helpWords) ||
      (words.contains("what") && words.contains("can") && words.contains("you") && words.contains("do"))

  def matchesListPlanets(words: List[String]): Boolean =
    containsAny(words, listWords) && words.contains("planets")

  def matchesRandomFact(words: List[String]): Boolean = {
    // Make this more flexible to match various ways of asking for a random fact
    (words.contains("random") && (words.contains("fact") || words.contains("facts"))) ||
    (words.contains("tell") && words.contains("fact")) ||
    (words.contains("give") && words.contains("fact")) ||
    words.mkString(" ") == "random fact" ||
    words.mkString(" ") == "fact" ||
    (words.contains("interesting") && words.contains("fact"))
  }

  def matchesQuiz(words: List[String]): Boolean =
    containsAny(words, quizWords) || (words.contains("test") && words.contains("knowledge"))

  def matchesCompare(input: String): Boolean =
    compareWords.exists(word => input.contains(word))

  def matchesCategory(words: List[String]): Boolean = {
    def categoryMatches(cat: String): Boolean =
      words.contains(cat) || (cat.contains(" ") && cat.split(" ").forall(part => words.contains(part)))
    val foundCategory = categories.exists(categoryMatches)
    (containsAny(words, listWords) && foundCategory) || foundCategory
  }

  def matchesPlanet(words: List[String]): Boolean =
    containsAny(words, planets)

  def extractCompareTopics(input: String): Option[(String, String)] = {
    val normalizedInput = input.toLowerCase.trim
    
    // Look for comparison patterns
    val patterns = List(
      "compare", "vs", "versus", "and", "between"
    )
    
    // Try to find two objects to compare
    val objects = patterns.foldLeft(normalizedInput) { (acc, pattern) =>
      acc.replace(pattern, "|")
    }.split("\\|").map(_.trim).filter(_.nonEmpty)
    
    if (objects.length >= 2) {
      Some((objects(0), objects(1)))
    } else None
  }

  def extractTopic(input: String): String = {
    val prefixes = List(
      "tell me about", "what is", "what are", "who is", "where is", "tell me",
      "explain", "describe", "talk about", "can you tell me about",
      "i want to know about", "information about", "details about"
    )

    def removePrefix(text: String, remainingPrefixes: List[String]): String = remainingPrefixes match {
      case Nil => text
      case prefix :: rest =>
        if (text.startsWith(prefix)) text.substring(prefix.length).trim
        else removePrefix(text, rest)
    }

    def removeFillerWords(text: String, fillers: List[String]): String = fillers match {
      case Nil => text
      case filler :: rest =>
        if (text.startsWith(filler)) removeFillerWords(text.substring(filler.length).trim, fillers)
        else removeFillerWords(text, rest)
    }

    val withoutPrefix = removePrefix(input, prefixes)
    val withoutPunctuation = withoutPrefix.replaceAll("[?!.,]+$", "").trim
    val fillerWords = List("please", "can you", "could you", "would you")
    removeFillerWords(withoutPunctuation, fillerWords)
  }

  def parseQuizMode(words: List[String], originalInput: String): (String, String, String) = {
    def containsExitWord(ws: List[String]): Boolean =
      ws.exists(exitWords.contains)

    if (containsExitWord(words) && (words.contains("quiz") || words.length == 1)) {
      (CMD_EXIT_QUIZ, "", "")
    } else if (words.contains("skip")) {
      (CMD_SKIP_QUESTION, "", "")
    } else {
      (CMD_ANSWER_QUIZ, originalInput, "")
    }
  }

  def parseRegularMode(words: List[String], originalInput: String): (String, String, String) = {
    // First check for casual conversation inputs
    val casualInput = originalInput.toLowerCase.trim
    if (List("i love you", "love you", "how are you", "how are you doing", "how are you today",
             "where are you", "where are you from", "what is your name", "who are you",
             "what can you do", "what do you do", "tell me a joke", "joke",
             "good morning", "good night", "thank you", "thanks",
             "who created you", "who made you", "do you have feelings",
             "are you human", "do you dream", "can you dream",
             "can you help me", "help me", "bye", "goodbye", "see you").contains(casualInput)) {
      return (CMD_UNKNOWN, casualInput, "")
    }

    // Then check for comparison commands specifically
    if (matchesCompare(originalInput)) {
      extractCompareTopics(originalInput) match {
        case Some((topic1, topic2)) => (CMD_COMPARE, topic1, topic2)
        case None => (CMD_UNKNOWN, originalInput, "")
      }
    } else if (matchesGreetings(words)) {
      (CMD_GREETINGS, "", "")
    } else if (matchesHelp(words)) {
      (CMD_HELP, "", "")
    } else if (matchesListPlanets(words)) {
      (CMD_LIST_PLANETS, "", "")
    } else if (matchesRandomFact(words)) {  // Move this check earlier in the sequence
      (CMD_RANDOM_FACT, "", "")
    } else if (matchesQuiz(words)) {
      (CMD_START_QUIZ, "", "")
    } else if (matchesCategory(words)) {
      def findCategory: String = {
        def categoryMatches(cat: String): Boolean =
          words.contains(cat) || (cat.contains(" ") && cat.split(" ").forall(part => words.contains(part)))

        categories.find(categoryMatches).getOrElse("unknown")
      }

      (CMD_LIST_CATEGORY, findCategory, "")
    } else if (matchesPlanet(words)) {
      def findPlanet: String = planets.find(words.contains).getOrElse("unknown").capitalize
      (CMD_ASK_ABOUT, findPlanet, "")
    } else {
      val topic = extractTopic(originalInput)
      if (topic.nonEmpty && !casualInput.contains("love") && !casualInput.contains("how are you")) {
        (CMD_ASK_ABOUT, topic.capitalize, "")
      } else {
        (CMD_UNKNOWN, originalInput, "")
      }
    }
  }
}

class QuizManagerImpl {
  import Constants._
  
  case class Question(text: String, options: List[String], correctAnswer: Option[String] = None)
  
  type QuizResponse = (String, List[String])
  type QuizAnswer = (Question, String, Boolean)
  type QuizState = Option[(String, List[Question], Int, List[QuizAnswer])]
  type QuizResults = List[(String, Int, Int)]
  type ApplicationState = (QuizState, QuizResults)
  type CommandResult = String

  private val synonymMap = Map(
    "milky way" -> List("galaxy", "our galaxy", "home galaxy", "milky", "way"),
    "mercury" -> List("smallest planet", "innermost planet", "first planet"),
    "mars" -> List("red planet", "fourth planet", "red"),
    "jupiter" -> List("largest planet", "biggest planet", "gas giant", "fifth planet"),
    "saturn" -> List("ringed planet", "sixth planet", "rings"),
    "venus" -> List("morning star", "evening star", "second planet", "hottest planet"),
    "earth" -> List("blue planet", "our planet", "third planet", "home planet"),
    "neptune" -> List("blue giant", "eighth planet", "ice giant"),
    "uranus" -> List("seventh planet", "ice giant", "sideways planet"),
    "sun" -> List("star", "solar", "our star", "central star"),
    "galaxy" -> List("star system", "stellar system", "star cluster"),
    "star" -> List("sun", "stellar object", "celestial body"),
    "planet" -> List("world", "celestial body", "celestial object")
  )

  // Initial state
  private var quizState: QuizState = None
  private var quizResults: QuizResults = Nil
  
  def resetQuizState(): Unit = {
    quizState = None
  }
  
  def isQuizActive(): Boolean = quizState.isDefined

  def selectQuizQuestions(topic: String): List[Question] = {
    val traditional = List(
      Question(
        "What is the name of our galaxy?",
        List("Butterfly Galaxy", "Milky Way Galaxy", "Spiral Galaxy", "Andromeda Galaxy"),
        Some("Milky Way Galaxy/Milky Way/Our Galaxy")
      ),
      Question(
        "What is the smallest planet in our solar system?",
        List("Mercury", "Mars", "Pluto", "Venus"),
        Some("Mercury/Smallest Planet/First Planet")
      ),
      Question(
        "Which planet is known as the Red Planet?",
        List("Jupiter", "Mars", "Venus", "Mercury"),
        Some("Mars/Red Planet/Fourth Planet")
      ),
      Question(
        "What is the largest planet in our solar system?",
        List("Neptune", "Jupiter", "Saturn", "Uranus"),
        Some("Jupiter/Largest Planet/Gas Giant")
      ),
      Question(
        "What is the approximate distance of Earth from the Sun?",
        List("149.6 million km", "200 million km", "100 million km", "300 million km"),
        Some("149.6 million km/150 million km/1 AU")
      ),
      Question(
        "Which planet is known for its beautiful rings?",
        List("Jupiter", "Mars", "Saturn", "Uranus"),
        Some("Saturn/Ringed Planet/Sixth Planet")
      ),
      Question(
        "What is the average surface temperature on Venus?",
        List("462°C", "100°C", "200°C", "300°C"),
        Some("462/460/462 degrees")
      )
    )

    val personal = List(
      Question(
        "What is your favorite planet in our solar system?",
        List("Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"),
        None
      ),
      Question(
        "Which celestial phenomenon would you most like to see?",
        List("Solar Eclipse", "Aurora Borealis", "Meteor Shower", "Supernova"),
        None
      ),
      Question(
        "If you could visit any place in space, where would you go?",
        List("Moon", "Mars", "Jupiter's Moons", "Saturn's Rings"),
        None
      ),
      Question(
        "Which space mission interests you the most?",
        List("Moon Landing", "Mars Colonization", "Deep Space Exploration", "Space Tourism"),
        None
      ),
      Question(
        "What aspect of astronomy fascinates you most?",
        List("Black Holes", "Alien Life", "Galaxy Formation", "Star Life Cycles"),
        None
      )
    )

    topic match {
      case "traditional" => traditional
      case "personal" => personal
      case _ => List(Question("Topic not found", List(""), None))
    }
  }

  private def normalizeNumber(input: String): String = {
    input.replaceAll("[^0-9.]+", "")
      .replaceAll(",", "")
      .trim
  }

  private def compareNumbers(answer: String, correct: String, tolerance: Double = 0.1): Boolean = {
    try {
      val answerNum = normalizeNumber(answer).toDouble
      val correctNum = normalizeNumber(correct).toDouble
      val difference = Math.abs(answerNum - correctNum)
      val percentDiff = difference / correctNum
      percentDiff <= tolerance
    } catch {
      case _: Exception => false
    }
  }

  private def checkSynonyms(userAnswer: String, correctAnswer: String): Boolean = {
    val userLower = userAnswer.toLowerCase
    val correctLower = correctAnswer.toLowerCase

    synonymMap.exists { case (key, synonyms) =>
      val allForms = key :: synonyms
      allForms.exists(syn => userLower.contains(syn)) &&
      allForms.exists(syn => correctLower.contains(syn))
    }
  }

  private def isNumericAnswer(answer: String): Boolean = {
    answer.matches(".*\\d+.*")
  }

  def presentQuestion(questions: List[Question], index: Int): String = {
    if (index < questions.length) {
      val q = questions(index)
      s"${q.text}\n\nOptions: ${q.options.mkString(", ")}"
    } else {
      "No more questions available."
    }
  }

  def processTopicSelection(message: String): CommandResult = {
    message.trim.toLowerCase match {
      case "traditional" | "1" =>
        val quizQuestions = selectQuizQuestions("traditional")
        quizState = Some(("traditional", quizQuestions, 0, List.empty[QuizAnswer]))
        val question = presentQuestion(quizQuestions, 0)
        s"Starting traditional astronomy quiz!\n\nQuestion: ${question}"

      case "personal" | "2" =>
        val quizQuestions = selectQuizQuestions("personal")
        quizState = Some(("personal", quizQuestions, 0, List.empty[QuizAnswer]))
        val question = presentQuestion(quizQuestions, 0)
        s"Starting personal preferences quiz!\n\nQuestion: ${question}"

      case _ =>
        "Please choose a valid quiz type: 1) Traditional, 2) Personal"
    }
  }

  private def levenshteinDistance(s1: String, s2: String): Int = {
    val dist = Array.ofDim[Int](s1.length + 1, s2.length + 1)
    
    for (i <- 0.to(s1.length)) dist(i)(0) = i
    for (j <- 0.to(s2.length)) dist(0)(j) = j
    
    for {
      j <- 1.to(s2.length)
      i <- 1.to(s1.length)
    } {
      dist(i)(j) = if (s1(i - 1) == s2(j - 1)) {
        dist(i - 1)(j - 1)
      } else {
        List(
          dist(i - 1)(j) + 1, // deletion
          dist(i)(j - 1) + 1, // insertion
          dist(i - 1)(j - 1) + 1 // substitution
        ).min
      }
    }
    
    dist(s1.length)(s2.length)
  }

  private def findClosestMatch(input: String, options: List[String]): Option[String] = {
    val normalizedInput = input.toLowerCase.replaceAll("[^a-z0-9]", "")
    options
      .map(opt => {
        val normalizedOpt = opt.toLowerCase.replaceAll("[^a-z0-9]", "")
        (opt, levenshteinDistance(normalizedInput, normalizedOpt))
      })
      .filter(_._2 <= 2) // Accept matches with distance of 2 or less
      .minByOption(_._2)
      .map(_._1)
  }

  private def validateQuizAnswer(answer: String, options: List[String]): Boolean = {
    val normalizedAnswer = answer.toLowerCase.trim
    val normalizedOptions = options.map(_.toLowerCase.trim)
    
    normalizedOptions.exists(opt => normalizedAnswer.contains(opt) || opt.contains(normalizedAnswer)) ||
    findClosestMatch(normalizedAnswer, options).isDefined
  }

  def handleQuizAnswer(answer: String): CommandResult = {
    quizState match {
      case Some((topic, questions, currentIndex, answers)) if currentIndex < questions.length =>
        val currentQuestion = questions(currentIndex)
        val options = currentQuestion.options
        val correctAnswer = currentQuestion.correctAnswer
        val userAnswer = answer.toLowerCase.trim

        // Input validation for empty answers
        if (userAnswer.isEmpty) {
          return s"${Red}Please provide an answer.${Reset}"
        }

        // For traditional quiz, validate against options and synonyms
        if (topic == "traditional") {
          if (!validateQuizAnswer(userAnswer, options) && 
              !correctAnswer.exists(correct => checkSynonyms(userAnswer, correct))) {
            return s"${Red}That's not quite right. Try another answer!${Reset}"
          }
        }

        // For personal quiz, validate against options with spelling tolerance
        if (topic == "personal") {
          if (!validateQuizAnswer(userAnswer, options)) {
            return s"${Red}Please provide a valid answer.${Reset}"
          }
        }

        val isCorrect = correctAnswer match {
          case Some(correct) =>
            topic match {
              case "traditional" =>
                val correctLower = correct.toLowerCase
                
                // Check for numerical answers first
                if (isNumericAnswer(correct) && isNumericAnswer(userAnswer)) {
                  compareNumbers(userAnswer, correct)
                } else {
                  // Multiple ways to be correct:
                  val directMatch = userAnswer.contains(correctLower) || correctLower.contains(userAnswer)
                  val synonymMatch = checkSynonyms(userAnswer, correct)
                  val optionMatch = options.exists(opt => {
                    val optLower = opt.toLowerCase
                    (userAnswer.contains(optLower) || optLower.contains(userAnswer)) &&
                    (optLower.contains(correctLower) || correctLower.contains(optLower))
                  })
                  
                  // Handle multiple correct answers (separated by '/')
                  val multipleCorrect = correct.split("/").map(_.trim.toLowerCase).exists(c =>
                    userAnswer.contains(c) || c.contains(userAnswer) || checkSynonyms(userAnswer, c)
                  )

                  // Consider special cases for astronomical terms
                  val specialCases = List(
                    userAnswer.contains("million") && correctLower.contains("million"),
                    userAnswer.contains("billion") && correctLower.contains("billion"),
                    userAnswer.contains("light year") && correctLower.contains("light year"),
                    userAnswer.contains("au") && correctLower.contains("astronomical unit")
                  ).exists(identity)

                  // Add fuzzy matching for spelling mistakes
                  val fuzzyMatch = findClosestMatch(userAnswer, correct.split("/").toList).isDefined

                  directMatch || synonymMatch || optionMatch || multipleCorrect || specialCases || fuzzyMatch
                }
              case _ =>
                // For personal questions, any valid option is correct (including close spelling matches)
                validateQuizAnswer(userAnswer, options)
            }
          case None =>
            // For personal questions, any valid option is correct (including close spelling matches)
            validateQuizAnswer(userAnswer, options)
        }

        val newAnswers = answers :+ (currentQuestion, answer, isCorrect)
        val nextIndex = currentIndex + 1

        if (nextIndex >= questions.length) {
          // Quiz completed
          quizState = Some((topic, questions, nextIndex, newAnswers))
          quizResults = updateQuizResults(quizResults, topic, newAnswers)
          val summary = generateQuizSummary(topic, newAnswers)

          val feedback = if (isCorrect) {
            "Correct!"
          } else {
            correctAnswer match {
              case Some(correct) if isNumericAnswer(correct) =>
                s"Incorrect. The answer should be around $correct (±10% accepted)"
              case Some(correct) if correct.contains("/") =>
                s"Incorrect. Any of these would be correct: ${correct.split("/").mkString(" or ")}"
              case Some(correct) =>
                s"Incorrect. The correct answer is: $correct"
              case None =>
                "Thank you for your answer!"
            }
          }

          s"$feedback\n\nQuiz completed!\n\n$summary"
        } else {
          // Move to next question
          quizState = Some((topic, questions, nextIndex, newAnswers))
          val nextQuestion = presentQuestion(questions, nextIndex)

          val feedback = if (isCorrect) {
            "Correct!"
          } else {
            correctAnswer match {
              case Some(correct) if isNumericAnswer(correct) =>
                s"Incorrect. The answer should be around $correct (±10% accepted)"
              case Some(correct) if correct.contains("/") =>
                s"Incorrect. Any of these would be correct: ${correct.split("/").mkString(" or ")}"
              case Some(correct) =>
                s"Incorrect. The correct answer is: $correct"
              case None =>
                "Thank you for your answer!"
            }
          }

          s"$feedback\n\nNext question:\n\nQuestion: $nextQuestion"
        }

      case _ =>
        "No active quiz question to answer. Try 'start quiz' to begin a new quiz."
    }
  }

  def updateQuizResults(
    results: QuizResults,
    topic: String,
    answers: List[QuizAnswer]
  ): QuizResults = {
    val correct = answers.count(_._3)
    val total = answers.length

    results.find(_._1 == topic) match {
      case Some(_) =>
        results.map {
          case (t, c, tot) if t == topic => (t, c + correct, tot + total)
          case other => other
        }
      case None =>
        results :+ (topic, correct, total)
    }
  }

  def generateQuizSummary(
    topic: String,
    answers: List[QuizAnswer]
  ): String = {
    val correct = answers.count(_._3)
    val total = answers.length
    val percentage = if (total > 0) (correct.toDouble / total) * 100 else 0

    topic match {
      case "traditional" =>
        s"""Quiz Summary:
           |Topic: Traditional Astronomy
           |Score: $correct out of $total (${percentage.toInt}%)
           |
           |Question Review:
           |${answers.map { case (q, ans, correct) =>
             s"- ${q.text}\n  Your answer: $ans\n  ${if (correct) "✓ Correct" else "✗ Incorrect"}"
           }.mkString("\n\n")}""".stripMargin
      case "personal" =>
        s"""Your Space Profile:
           |${answers.map { case (q, ans, _) =>
             s"- ${q.text}\n  Your choice: $ans"
           }.mkString("\n\n")}""".stripMargin
      case _ =>
        s"Quiz Summary:\nTopic: $topic\nScore: $correct out of $total (${percentage.toInt}%)"
    }
  }

  def endQuiz(): CommandResult = {
    quizState match {
      case Some((topic, _, _, answers)) =>
        val summary = if (answers.nonEmpty) {
          generateQuizSummary(topic, answers)
        } else {
          "Quiz ended without answering any questions."
        }
        quizState = None
        s"Quiz ended.\n\n$summary"

      case None =>
        "No active quiz to end."
    }
  }

  def getQuizSummary(): String = {
    if (quizResults.isEmpty) {
      "No quiz results available yet."
    } else {
      val summaries = quizResults.map { case (topic, correct, total) =>
        val percentage = if (total > 0) (correct.toDouble / total) * 100 else 0
        s"$topic: $correct out of $total (${percentage.toInt}%)"
      }
      "Quiz History:\n" + summaries.mkString("\n")
    }
  }

  def handleMessage(message: String): CommandResult = {
    val isAwaitingTopicSelection = quizState.exists(quiz => quiz._1 == "topic_selection" && quiz._3 == 0)
    val isAwaitingAnswer = quizState.isDefined && quizState.exists(quiz => quiz._3 < quiz._2.length)

    if (isAwaitingTopicSelection) {
      return processTopicSelection(message)
    }

    if (isAwaitingAnswer) {
      return handleQuizAnswer(message)
    }

    message.toLowerCase.trim match {
      case m if m.startsWith("start quiz") || m.startsWith("quiz me") || m == "quiz" =>
        quizState = Some(("topic_selection", List.empty[Question], 0, List.empty[QuizAnswer]))
        "Please choose a quiz type:\n1) Traditional astronomy quiz\n2) Personal preferences quiz"

      case "quiz topics" | "show topics" | "available quizzes" =>
        "Available quiz topics: traditional, personal"

      case "quiz results" | "show results" | "quiz summary" =>
        getQuizSummary()

      case "reset quiz" | "restart quiz" =>
        quizState = None
        "Quiz reset. Start a new quiz with 'start quiz'."

      case "end quiz" | "stop quiz" | "exit quiz" =>
        endQuiz()

      case "help" | "quiz help" =>
        """Quiz commands available:
           |• start quiz - Begin a new quiz
           |• quiz topics - Show available quiz topics
           |• quiz results - View your quiz history
           |• reset quiz - Start over
           |• end quiz - End current quiz
           |
           |During the quiz:
           |• Type your answer directly
           |• Partial answers are accepted for traditional quiz
           |• Personal questions have no wrong answers""".stripMargin

      case _ =>
        if (quizState.isDefined) {
          "Please answer the current question or type 'end quiz' to stop."
        } else {
          "Try 'start quiz' to begin or 'help' for commands."
        }
    }
  }
}

object QuizManager {
  def apply(): QuizManagerImpl = new QuizManagerImpl()
}

object ResponseGenerator {
  import Constants._
  
  def getSolarSystemFact(): String = {
    val allData = DataLoader.spaceObjectsData
    
    if (allData.isEmpty) {
      // Fallback to static facts if no data is available
      val staticFacts = List(
        "Mercury is the smallest and innermost planet in the Solar System.",
        "Venus is the hottest planet in the Solar System, with surface temperatures reaching 462°C.",
        "Earth is the only planet known to harbor life.",
        "Mars has the largest volcano in the solar system, Olympus Mons.",
        "Jupiter's Great Red Spot is a giant storm that has been raging for at least 400 years.",
        "Saturn's rings are made mostly of ice and rock.",
        "Uranus rotates on its side, unlike any other planet in our solar system.",
        "Neptune has the strongest winds of any planet, reaching speeds of 2,100 km/hour.",
        "The Sun contains 99.86% of the mass in the Solar System.",
        "Pluto was once considered the ninth planet until it was reclassified as a dwarf planet in 2006."
      )
      staticFacts(Random.nextInt(staticFacts.length))
    } else {
      // Get a random object and its interesting fact
      val randomObject = allData.toList(Random.nextInt(allData.size))
      val (name, data) = randomObject
      
      // Try to get an interesting fact from various fields
      val possibleFacts = List(
        data.get("notable_features"),
        data.get("description"),
        Some(s"${name.capitalize} has a diameter of ${data.getOrElse("diameter", "unknown size")}"),
        data.get("atmosphere").map(atm => s"${name.capitalize}'s atmosphere consists of ${atm}"),
        data.get("surface_temperature").map(temp => s"The temperature on ${name.capitalize} is ${temp}")
      ).flatten
      
      possibleFacts(Random.nextInt(possibleFacts.length))
    }
  }
  
  def getPlanets(): List[String] = Constants.planets.map(_.capitalize)

  def getFacts(topic: String): Option[Map[String, String]] = {
    // Try to get data from both sources
    val jsonData = DataLoader.astronomyData.get(topic.toLowerCase)
    val csvData = DataLoader.spaceObjectsData.get(topic.toLowerCase)
    
    (jsonData, csvData) match {
      case (Some(json), Some(csv)) => 
        // Merge data from both sources, CSV data takes precedence
        Some(json ++ csv)
      case (Some(json), None) => Some(json)
      case (None, Some(csv)) => Some(csv)
      case (None, None) => None
    }
  }
  
  def formatPlanetResponse(planet: String, dataSource: AstronomyData): String = {
    val info = getFacts(planet.toLowerCase)
    if (info.isEmpty) {
      s"Sorry, I don't have information about $planet."
    } else {
      s"$planet:\n" + info.get.map { case (k, v) =>
        s"- ${k.replace("_", " ").capitalize}: $v"
      }.mkString("\n")
    }
  }
  
  def formatCategoryResponse(category: String): String = {
    category match {
      case "stars" => "Some notable stars: Sun, Sirius, Vega, Betelgeuse, Proxima Centauri"
      case "galaxies" => "Types of galaxies: Spiral (like our Milky Way), Elliptical, Irregular, and Lenticular"
      case "black holes" => "Black holes are regions where gravity is so strong that nothing can escape, not even light!"
      case _ => s"$category is a fascinating topic in astronomy!"
    }
  }
  
  def getRandomUnknownResponse(): String = {
    val responses = List(
      "I'm not sure I understand. Try asking about planets, stars, or galaxies.",
      "Could you rephrase that? Try 'help' to see what I can do.",
      "I didn't catch that. Maybe try asking about a specific astronomy topic?"
    )
    responses(Random.nextInt(responses.length))
  }
  
  def getRandomGreeting(): String = {
    val greetings = List(
      "Hello! How can I help with astronomy today?",
      "Hi! What would you like to know about space?",
      "Welcome! Ask me anything about the universe!",
      "Hey there! Ready to explore the stars?"
    )
    greetings(Random.nextInt(greetings.length))
  }

  def getHelpMessage(): String = {
    "I can help with:\n- Ask about planets: 'tell me about Mars'\n- Compare: 'compare Earth and Mars'\n- Lists: 'list planets'\n- Facts: 'random fact'\n- Quiz: 'start quiz'"
  }

  def greetUser(name: String): String = {
    s"Hello, $name! I'm CHATURN, your astronomy chatbot. Ask me about planets, stars, or try 'help' for commands!"
  }
  
  def formatQuizResponse(response: String): String = {
    response match {
      case r if r.contains("Correct!")                                         => s"${Green}$r${Reset}"
      case r if r.contains("Incorrect") || r.contains("The correct answer is") => s"${Red}$r${Reset}"
      case r if r.startsWith("Question:") || r.contains("Question:") =>
        val parts = r.split("Question:", 2)
        if (parts.length > 1) {
          val intro        = parts(0).trim
          val questionPart = parts(1).trim
          val (questionText, options) = if (questionPart.contains("\n\n")) {
            val split = questionPart.split("\n\n", 2)
            (split(0), split(1))
          } else {
            (questionPart, "")
          }
          s"${intro}${Cyan}Question:${Reset} $questionText\n\n${Yellow}$options${Reset}"
        } else {
          s"${Cyan}$r${Reset}"
        }
      case r if r.contains("Quiz Summary") || r.contains("Your Personalized Space Profile") => s"${Yellow}$r${Reset}"
      case r if r.trim.isEmpty => s"${Cyan}No response available.${Reset}"
      case r                   => s"${Cyan}$r${Reset}"
    }
  }

  private def compareProperties(obj1: Map[String, String], obj2: Map[String, String]): List[String] = {
    val commonKeys = obj1.keySet.intersect(obj2.keySet)
    val comparisons = commonKeys.toList.flatMap { key =>
      val value1 = obj1(key)
      val value2 = obj2(key)
      
      key match {
        case "name" => None // Skip name comparison
        case "type" => Some(s"Type: ${obj1("name")} is a ${value1}, while ${obj2("name")} is a ${value2}")
        case "diameter" => 
          try {
            val d1 = value1.replaceAll("[^0-9.]", "").toDouble
            val d2 = value2.replaceAll("[^0-9.]", "").toDouble
            Some(s"Size: ${obj1("name")} has a diameter of ${value1}, while ${obj2("name")} has a diameter of ${value2}" +
                 (if (d1 > d2) s" (${obj1("name")} is larger)" 
                  else if (d2 > d1) s" (${obj2("name")} is larger)"
                  else " (they are the same size)"))
          } catch {
            case _: Exception => Some(s"Size: ${obj1("name")}: ${value1}, ${obj2("name")}: ${value2}")
          }
        case "distance_from_sun" =>
          try {
            val d1 = value1.replaceAll("[^0-9.]", "").toDouble
            val d2 = value2.replaceAll("[^0-9.]", "").toDouble
            Some(s"Distance from Sun: ${obj1("name")} is ${value1} from the Sun, while ${obj2("name")} is ${value2} from the Sun" +
                 (if (d1 > d2) s" (${obj1("name")} is farther)" 
                  else if (d2 > d1) s" (${obj2("name")} is farther)"
                  else " (they are at the same distance)"))
          } catch {
            case _: Exception => Some(s"Distance from Sun: ${obj1("name")}: ${value1}, ${obj2("name")}: ${value2}")
          }
        case "mass" =>
          Some(s"Mass: ${obj1("name")} has a mass of ${value1}, while ${obj2("name")} has a mass of ${value2}")
        case "description" =>
          Some(s"Description:\n- ${obj1("name")}: ${value1}\n- ${obj2("name")}: ${value2}")
        case _ =>
          Some(s"${key.replace("_", " ").capitalize}: ${obj1("name")}: ${value1}, ${obj2("name")}: ${value2}")
      }
    }

    if (comparisons.isEmpty) {
      List(s"Sorry, I don't have enough comparable data for ${obj1.getOrElse("name", "unknown")} and ${obj2.getOrElse("name", "unknown")}")
    } else {
      comparisons
    }
  }

  private def generateUniqueFeatures(obj: Map[String, String], otherObj: Map[String, String]): List[String] = {
    val uniqueKeys = obj.keySet.diff(otherObj.keySet)
    uniqueKeys.toList.map { key =>
      s"${key.replace("_", " ").capitalize}: ${obj("name")} - ${obj(key)}"
    }
  }

  def formatComparisonResponse(topic1: String, topic2: String, dataSource: AstronomyData): String = {
    val data1 = getFacts(topic1.toLowerCase)
    val data2 = getFacts(topic2.toLowerCase)

    (data1, data2) match {
      case (Some(obj1), Some(obj2)) =>
        val comparisons = compareProperties(obj1, obj2)
        val uniqueFeatures1 = generateUniqueFeatures(obj1, obj2)
        val uniqueFeatures2 = generateUniqueFeatures(obj2, obj1)

        val comparisonText = comparisons.mkString("\n")
        val unique1Text = if (uniqueFeatures1.nonEmpty) s"\n\nUnique features of ${obj1("name")}:\n" + uniqueFeatures1.map("- " + _).mkString("\n") else ""
        val unique2Text = if (uniqueFeatures2.nonEmpty) s"\n\nUnique features of ${obj2("name")}:\n" + uniqueFeatures2.map("- " + _).mkString("\n") else ""

        s"Comparison between ${obj1("name")} and ${obj2("name")}:\n\n$comparisonText$unique1Text$unique2Text"

      case (None, None) =>
        s"Sorry, I don't have information about either $topic1 or $topic2."
      case (None, _) =>
        s"Sorry, I don't have information about $topic1."
      case (_, None) =>
        s"Sorry, I don't have information about $topic2."
    }
  }

  def compareBetween(obj1Name: String, obj2Name: String): String = {
    val data = DataLoader.spaceObjectsData
    
    (data.get(obj1Name.toLowerCase), data.get(obj2Name.toLowerCase)) match {
      case (Some(obj1), Some(obj2)) =>
        val comparisons = List(
          compareBasicInfo(obj1, obj2),
          comparePhysicalCharacteristics(obj1, obj2),
          compareEnvironment(obj1, obj2),
          compareOrbitalCharacteristics(obj1, obj2)
        ).flatten.filter(_.nonEmpty)

        if (comparisons.isEmpty) {
          s"${Red}No comparable data found between ${obj1Name} and ${obj2Name}.${Reset}"
        } else {
          s"""${Yellow}Comparison between ${obj1Name.capitalize} and ${obj2Name.capitalize}:${Reset}
             |
             |${comparisons.mkString("\n\n")}""".stripMargin
        }

      case (None, None) =>
        s"${Red}Sorry, I don't have information about either $obj1Name or $obj2Name.${Reset}"
      case (None, _) =>
        s"${Red}Sorry, I don't have information about $obj1Name.${Reset}"
      case (_, None) =>
        s"${Red}Sorry, I don't have information about $obj2Name.${Reset}"
    }
  }

  private def compareBasicInfo(obj1: Map[String, String], obj2: Map[String, String]): Option[String] = {
    val name1 = obj1("name").capitalize
    val name2 = obj2("name").capitalize
    
    val typeComparison = for {
      type1 <- obj1.get("type")
      type2 <- obj2.get("type")
    } yield s"${Cyan}Type:${Reset} $name1 is a $type1, while $name2 is a $type2"

    val descComparison = for {
      desc1 <- obj1.get("description")
      desc2 <- obj2.get("description")
    } yield s"""${Cyan}Description:${Reset}
               |• $name1: $desc1
               |• $name2: $desc2""".stripMargin

    Some((List(typeComparison, descComparison).flatten).mkString("\n\n"))
      .filter(_.nonEmpty)
  }

  private def comparePhysicalCharacteristics(obj1: Map[String, String], obj2: Map[String, String]): Option[String] = {
    val name1 = obj1("name").capitalize
    val name2 = obj2("name").capitalize
    
    val comparisons = List(
      for {
        d1 <- obj1.get("diameter")
        d2 <- obj2.get("diameter")
      } yield s"${Cyan}Size:${Reset} $name1: $d1, $name2: $d2",
      
      for {
        m1 <- obj1.get("mass")
        m2 <- obj2.get("mass")
      } yield s"${Cyan}Mass:${Reset} $name1: $m1, $name2: $m2",
      
      for {
        sc1 <- obj1.get("surface_composition")
        sc2 <- obj2.get("surface_composition")
      } yield s"${Cyan}Surface Composition:${Reset} $name1: $sc1, $name2: $sc2"
    ).flatten

    if (comparisons.isEmpty) None
    else Some(comparisons.mkString("\n"))
  }

  private def compareEnvironment(obj1: Map[String, String], obj2: Map[String, String]): Option[String] = {
    val name1 = obj1("name").capitalize
    val name2 = obj2("name").capitalize
    
    val comparisons = List(
      for {
        t1 <- obj1.get("temperature")
        t2 <- obj2.get("temperature")
      } yield s"${Cyan}Temperature:${Reset} $name1: $t1, $name2: $t2",
      
      for {
        a1 <- obj1.get("atmosphere")
        a2 <- obj2.get("atmosphere")
      } yield s"${Cyan}Atmosphere:${Reset} $name1: $a1, $name2: $a2",
      
      for {
        m1 <- obj1.get("moons")
        m2 <- obj2.get("moons")
      } yield s"${Cyan}Moons:${Reset} $name1: $m1, $name2: $m2"
    ).flatten

    if (comparisons.isEmpty) None
    else Some(comparisons.mkString("\n"))
  }

  private def compareOrbitalCharacteristics(obj1: Map[String, String], obj2: Map[String, String]): Option[String] = {
    val name1 = obj1("name").capitalize
    val name2 = obj2("name").capitalize
    
    val comparisons = List(
      for {
        d1 <- obj1.get("distance_from_sun")
        d2 <- obj2.get("distance_from_sun")
      } yield s"${Cyan}Distance from Sun:${Reset} $name1: $d1, $name2: $d2",
      
      for {
        p1 <- obj1.get("orbital_period")
        p2 <- obj2.get("orbital_period")
      } yield s"${Cyan}Orbital Period:${Reset} $name1: $p1, $name2: $p2",
      
      for {
        r1 <- obj1.get("rotation_period")
        r2 <- obj2.get("rotation_period")
      } yield s"${Cyan}Rotation Period:${Reset} $name1: $r1, $name2: $r2"
    ).flatten

    if (comparisons.isEmpty) None
    else Some(comparisons.mkString("\n"))
  }

  private def UnexpectedInput(input: String): String = {
    input.toLowerCase.trim match {
      case "i love you" | "love you" => s"${Yellow}That's sweet! I love astronomy, and I'm here to share that passion with you!${Reset}"
      case "how are you" | "how are you doing" | "how are you today" => 
        s"${Cyan}I'm functioning perfectly and excited to explore the cosmos with you! How can I help?${Reset}"
      case "where are you" | "where are you from" => 
        s"${Cyan}I exist in the digital cosmos, ready to help you explore the real one!${Reset}"
      case "what is your name" | "who are you" => 
        s"${Cyan}I'm CHATURN, your friendly astronomy chatbot! I'm here to help you learn about space.${Reset}"
      case "what can you do" | "what do you do" => getHelpMessage()
      case "tell me a joke" | "joke" => 
        val jokes = List(
          "Why did the astronaut break up with the star? Because she needed some space!",
          "What kind of songs do planets sing? Nep-tunes!",
          "Why did Mars break up with Saturn? Because it had too many rings!",
          "What do you call a star that doesn't shower? A smelly dwarf!",
          "Why did the sun go to school? To get brighter!"
        )
        s"${Yellow}${jokes(Random.nextInt(jokes.length))}${Reset}"
      case "good morning" => s"${Green}Good morning! The stars may have faded, but space is still fascinating!${Reset}"
      case "good night" => s"${Blue}Good night! Perfect time for stargazing!${Reset}"
      case "thank you" | "thanks" => s"${Green}You're welcome! Feel free to ask more about astronomy!${Reset}"
      case "who created you" | "who made you" => 
        s"${Cyan}I was created by Team Chaturn: Mohamed, Dania, Maroska, and Jana.${Reset}"
      case "do you have feelings" | "are you human" => 
        s"${Purple}I'm an AI focused on astronomy. While I don't have feelings, I have a deep appreciation for the cosmos!${Reset}"
      case "do you dream" | "can you dream" => 
        s"${Yellow}I don't dream, but I can help make your dreams of understanding the universe come true!${Reset}"
      case "can you help me" | "help me" => 
        s"${Green}Of course! I'm here to help you explore astronomy. Try 'help' to see what I can do.${Reset}"
      case "bye" | "goodbye" | "see you" => 
        s"${Yellow}Goodbye! Come back soon to explore more of the cosmos!${Reset}"
      case "" => s"${Red}Please type something.${Reset}"
      case _ => getRandomUnknownResponse()
    }
  }

  def respond(
    command: String,
    dataSource: AstronomyData = DataLoader.astronomyData,
    analytics: Analytics,
    quizManager: QuizManager
  ): String = {
    if (command.startsWith("unknown_")) {
      val query = command.substring("unknown_".length)
      UnexpectedInput(query)
    } else if (command == "help") {
      getHelpMessage()
    } else if (command == "listplanets") {
      s"The planets in our solar system are: ${getPlanets().mkString(", ")}"
    } else if (command.startsWith("listcategory_")) {
      val category = command.substring("listcategory_".length)
      formatCategoryResponse(category)
    } else if (command == "randomfact") {
      getSolarSystemFact()
    } else if (command.startsWith("askabout_")) {
      val topic = command.substring("askabout_".length)
      formatPlanetResponse(topic, dataSource)
    } else if (command.startsWith("compare_")) {
      val parts = command.substring("compare_".length).split("_")
      if (parts.length >= 2) {
        compareBetween(parts(0), parts(1))
      } else {
        getRandomUnknownResponse()
      }
    } else if (command == "greetings") {
      getRandomGreeting()
    } else {
      getRandomUnknownResponse()
    }
  }
}

object Main {
  import Colors._
  
  def main(args: Array[String]): Unit = {
    displayWelcomeBanner()
    
    // Initialize components
    val config = Config.load
    val dataSource = initializeDataSource()
    val analytics = new AnalyticsImpl()
    val quizManager = QuizManager()
    val userName = promptUserName()
    
    println(s"\n${Yellow}Welcome, $userName, to the CHATURN Astronomy Chatbot!${Reset}")
    println(s"${Cyan}Type 'help' to see available commands or 'exit' to quit.${Reset}")
    
    mainLoop(dataSource, analytics, quizManager, userName)
  }
  
  private def initializeDataSource(): AstronomyData = {
    println(s"${Cyan}Initializing astronomy database...${Reset}")
    
    // Check if the astronomy.json file exists
    val file = new java.io.File("astronomy.json")
    if (!file.exists()) {
      // If file doesn't exist, create a sample astronomy.json file
      println(s"${Yellow}No astronomy data file found. Creating sample data...${Reset}")
      createSampleDataFile()
    }
    
    // Load data from the file
    val data = DataLoader.loadAstronomyData("astronomy.json")
    if (data.isEmpty) {
      println(s"${Red}Warning: Could not load astronomy data. Some features may be limited.${Reset}")
    } else {
      println(s"${Green}Loaded ${data.size} astronomy objects.${Reset}")
    }
    data
  }

  /*private def LoveYou(s1 : String): String = {
    

  }*/
  
  private def createSampleDataFile(): Unit = {
    import java.io.{FileWriter, PrintWriter}
    
    val sampleData = """[
      {
        "name": "Mars",
        "type": "planet",
        "diameter": "6,779 km",
        "mass": "6.39 × 10^23 kg",
        "distance_from_sun": "227.9 million km",
        "description": "Mars is the fourth planet from the Sun and is known as the 'Red Planet' due to its reddish appearance."
      },
      {
        "name": "Jupiter",
        "type": "planet",
        "diameter": "139,820 km",
        "mass": "1.898 × 10^27 kg",
        "distance_from_sun": "778.5 million km",
        "description": "Jupiter is the fifth planet from the Sun and the largest in the Solar System."
      },
      {
        "name": "Saturn",
        "type": "planet",
        "diameter": "116,460 km",
        "mass": "5.683 × 10^26 kg",
        "distance_from_sun": "1.434 billion km",
        "description": "Saturn is the sixth planet from the Sun and is known for its prominent ring system."
      },
      {
        "name": "Venus",
        "type": "planet",
        "diameter": "12,104 km",
        "mass": "4.867 × 10^24 kg",
        "distance_from_sun": "108.2 million km",
        "description": "Venus is the second planet from the Sun and is the hottest planet in our solar system."
      },
      {
        "name": "Mercury",
        "type": "planet",
        "diameter": "4,879 km",
        "mass": "3.285 × 10^23 kg",
        "distance_from_sun": "57.91 million km",
        "description": "Mercury is the smallest and innermost planet in the Solar System."
      },
      {
        "name": "Earth",
        "type": "planet",
        "diameter": "12,742 km",
        "mass": "5.972 × 10^24 kg",
        "distance_from_sun": "149.6 million km",
        "description": "Earth is the third planet from the Sun and the only astronomical object known to harbor life."
      },
      {
        "name": "Neptune",
        "type": "planet",
        "diameter": "49,244 km",
        "mass": "1.024 × 10^26 kg",
        "distance_from_sun": "4.495 billion km",
        "description": "Neptune is the eighth and farthest known planet from the Sun in the Solar System."
      },
      {
        "name": "Uranus",
        "type": "planet",
        "diameter": "50,724 km",
        "mass": "8.681 × 10^25 kg",
        "distance_from_sun": "2.871 billion km",
        "description": "Uranus is the seventh planet from the Sun and has the third-largest diameter in our solar system."
      },
      {
        "name": "Pluto",
        "type": "dwarf planet",
        "diameter": "2,377 km",
        "mass": "1.309 × 10^22 kg",
        "distance_from_sun": "5.9 billion km",
        "description": "Pluto is a dwarf planet in the Kuiper belt. It was classified as the ninth planet from the Sun until 2006."
      }
    ]"""
    
    Try {
      val writer = new PrintWriter(new FileWriter("astronomy.json"))
      try {
        writer.write(sampleData)
      } finally {
        writer.close()
      }
    } match {
      case Success(_) => println(s"${Green}Sample astronomy data created successfully.${Reset}")
      case Failure(e) => println(s"${Red}Error creating sample data: ${e.getMessage}${Reset}")
    }
  }
  
  private def promptUserName(): String = {
    print(s"${Green}Please enter your name: ${Reset}")
    scala.io.StdIn.readLine().trim.take(50) match {
      case name if name.nonEmpty => name
      case _                     => "Space Explorer"
    }
  }
  
  private def displayWelcomeBanner(): Unit = {
    println(s"""
               |${Cyan}=======================================${Reset}
               |${Yellow}${Colors.Blink}    CHATURN Astronomy Chatbot${Reset}
               |${Cyan}=======================================${Reset}
               |${Yellow}Team:${Reset} Chaturn
               |${Yellow}Members:${Reset} Mohamed, Dania, Maroska, Jana
               |${Cyan}
               |                                                                    ..;===+.
               |                                                                .:=iiiiii=+=
               |                                                             .=i))=;::+)i=+,
               |                                                          ,=i);)I)))I):=i=;
               |                                                       .=i==))))ii)))I:i++
               |                                                     +)+))iiiiiiii))I=i+:''  
               |                                .,:;;++++++;:,.       )iii+:::;iii))+i='
               |                             .:;++=iiiiiiiiii=++;.    =::,,,:::=i));=+''  
               |                           ,;+==ii)))))))))))ii==+;,      ,,,:=i))+=:
               |                         ,;+=ii))))))IIIIII))))ii===;.    ,,:=i)=i+
               |                        ;+=ii)))IIIIITIIIIII))))iiii=+,   ,:=));=,
               |                      ,+=i))IIIIIITTTTTITIIIIII)))I)i=+,,:+i)=i+
               |                     ,+i))IIIIIITTTTTTTTTTTTI))IIII))i=::i))i='
               |                    ,=i))IIIIITLLTTTTTTTTTTIITTTTIII)+;+i)+i`
               |                    =i))IIITTLTLTTTTTTTTTIITTLLTTTII+:i)ii:''
               |                   +i))IITTTLLLTTTTTTTTTTTTLLLTTTT+:i)))=,
               |                   =))ITTTTTTTTTTTTLTTTTTTLLLLLLTi:=)IIiii;
               |                  .i)IIITTTTTTTTLTTTITLLLLLLLT);=)I)))))i;
               |                  :))IIITTTTTLTTTTTTLLHLLLLL);=)II)IIIIi=:
               |                  :i)IIITTTTTTTTTLLLHLLHLL)+=)II)ITTTI)i=
               |                  .i)IIITTTTITTLLLHHLLLL);=)II)ITTTTII)i+
               |                  =i)IIIIIITTLLLLLLHLL=:i)II)TTTTTTIII)i''
               |                +i)i)))IITTLLLLLLLLT=:i)II)TTTTLTTIII)i;
               |              +ii)i:)IITTLLTLLLLT=;+i)I)ITTTTLTTTII))i;
               |             =;)i=:,=)ITTTTLTTI=:i))I)TTTLLLTTTTTII)i;
               |           +i)ii::,  +)IIITI+:+i)I))TTTTLLTTTTTII))=,
               |         :=;)i=:,,    ,i++::i))I)ITTTTTTTTTTIIII)=+''  
               |       .+ii)i=::,,   ,,::=i)))iIITTTTTTTTIIIII)=+  
               |      ,==)ii=;:,,,,:::=ii)i)iIIIITIIITIIII))i+:'  
               |     +=:))i==;:::;=iii)+)=  `:i)))IIIII)ii+'  
               |   .+=:))iiiiiiii)))+ii;  
               |  .+=;))iiiiii)));ii+  
               | .+=i:)))))))=+ii+  
               |.+==i+::::=)i=;  
               |,+==iiiiii+,  
               |`+=+++;`  
               |${Reset}
    """.stripMargin)
  }
  
  private def mainLoop(dataSource: AstronomyData, analytics: Analytics, quizManager: QuizManager, userName: String): Unit = {
    var running = true
    var isQuizMode = false
    
    while (running) {
      val prompt = if (isQuizMode) s"${Green}Quiz> ${Reset}" else s"${Green}CHATURN> ${Reset}"
      print(prompt)
      
      val input = scala.io.StdIn.readLine()
      
      if (input == null || input.trim.toLowerCase == "exit") {
        running = false
        val message = if (isQuizMode) {
          val response = quizManager.handleMessage("end quiz")
          quizManager.resetQuizState()
          s"${Yellow}Goodbye! Come back to explore the cosmos!${Reset}"
        } else {
          s"${Yellow}Goodbye! Come back to explore the cosmos!${Reset}"
        }
        println(message)
      } else if (input.trim.isEmpty) {
        println(s"${Red}Please type something!${Reset}")
      } else {
        val normalizedInput = input.trim.toLowerCase
        val response = normalizedInput match {
          case cmd if (cmd.startsWith("start quiz") || cmd == "quiz" || cmd.startsWith("start an astronomy quiz")) && !isQuizMode =>
            isQuizMode = true
            quizManager.handleMessage(input)
            
          case cmd if List("end quiz", "stop quiz", "exit quiz", "quit","exit").contains(cmd) && isQuizMode =>
            val resp = quizManager.handleMessage(input)
            isQuizMode = false
            quizManager.resetQuizState()
            resp
            
          case _ if isQuizMode =>
            val resp = quizManager.handleMessage(input)
            isQuizMode = quizManager.isQuizActive()
            if (!isQuizMode) {
              quizManager.resetQuizState()
            }
            resp
            
          case _ =>
            val command = InputParser.parseInput(input, isQuizMode)
            analytics.logInteraction(command)
            ResponseGenerator.respond(command, dataSource, analytics, quizManager)
        }
        
        // Format and output response
        val formattedResponse = if (isQuizMode || normalizedInput.startsWith("start quiz")) {
          ResponseGenerator.formatQuizResponse(response)
        } else {
          s"${Cyan}CHATURN>${Reset} $userName, $response"
        }
        
        println(formattedResponse)
      }
    }
  }
}

// Analytics class implementation
class AnalyticsImpl {
  import scala.collection.mutable.{Map => MutableMap}
  
  // Map to store command counts
  private val commandCounts: MutableMap[String, Int] = MutableMap.empty
  private var totalInteractions: Int = 0
  
  // Method to log interactions
  def logInteraction(command: String): Unit = {
    commandCounts.updateWith(command) {
      case Some(count) => Some(count + 1)
      case None => Some(1)
    }
    totalInteractions += 1
    println(s"Logged interaction: $command")
  }
  
  // Get the total number of interactions
  def getTotalInteractions: Int = totalInteractions
  
  // Get the most frequent command
  def getMostFrequentCommand: Option[(String, Int)] = {
    if (commandCounts.isEmpty) None
    else Some(commandCounts.maxBy(_._2))
  }
  
  // Get all command statistics
  def getCommandStatistics: Map[String, Int] = commandCounts.toMap
}

// Analytics object factory
object AnalyticsImpl {
  def apply(): AnalyticsImpl = new AnalyticsImpl()
}