# 🌌 Chaturn – The Astronomy Chatbot

An interactive, quiz-enabled chatbot built in **Scala** and integrated with a **Python GUI/Web Interface**, designed to help users learn about space in a fun, conversational way.

---

## 📚 Overview

**Chaturn** simulates intelligent space-themed conversations, answers astronomy-related questions, and delivers quizzes in both **traditional** (knowledge-based) and **personality** formats. Designed with **functional programming principles**, it combines science with friendly interaction.

---

## ✨ Key Features

- 🧠 Functional Programming in Scala (pattern matching, pure functions, immutability)
- 🪐 Intelligent conversation handling about planets, stars, and astronomy topics
- 🧩 Spelling correction and synonym matching (e.g., "plannet" → "planet")
- 🎯 Two quiz modes:
  - **Traditional Quiz** – factual multiple-choice questions
  - **Personality Quiz** – playful, personalized outcomes (e.g., "Which planet are you?")
- 📊 Quiz tracking and performance history
- 🌈 Color-coded feedback (green for correct, red for incorrect)
- 📤 Web interface integration via Python backend
- 🔀 Random facts after each quiz or on demand
- 🔁 Ability to restart, skip, or exit quizzes at any time

---

## 🚀 How to Use

### 1. Launch the Chatbot
- Use either the **terminal** or **web server** to start Chaturn.
- The chatbot greets you and prompts your first input.

### 2. Start Chatting
- Ask questions like:
  - `"What is a black hole?"`
  - `"Tell me about Mars"`
  - `"Compare Earth and Venus"`

### 3. Take a Quiz
- Type `"Start quiz"` and choose:
  - `"traditional"` for science-based questions
  - `"personal"` for space-themed personality results
- Receive instant feedback and explanations

### 4. Special Commands
- `exit` – Exit the quiz or chatbot
- `quiz history` – View your past performance
- `tell me a fun fact` – Get a cool space fact
- `help` – View all available commands

---

## 🛠️ Technologies

- **Scala** for chatbot logic and core architecture
- **Python (Flask/GUI)** for frontend web interface
- **JSON/CSV** data sources for facts and objects
- **Color-coded terminal output** for feedback and interaction

---

## 📂 Modules & Structure

- `Main` – Entry point to launch the chatbot
- `InputParser` – Interprets and classifies user input
- `ResponseGenerator` – Generates dynamic replies, facts, and comparisons
- `QuizManager` – Handles quiz logic, states, and scoring
- `Analytics` – Logs user interaction stats
- `DataLoader` – Loads and merges astronomy data from JSON/CSV

---

## 👥 Team Members & Contributions

| Name             | Role                                                  |
|------------------|--------------------------------------------------------|
| **Maroska Osama** | Core logic, quiz state, synonym handling, report author |
| **Mohamed Mostafa** | Web server integration, backend/frontend communication |
| **Dania Hassan**  | Input parsing, edge case handling, quiz validation    |
| **Jana Abdelmoniem** | Presentation creation, input parsing support, UX testing |

---

## 🧪 Testing & Validation

Extensive testing was conducted for:
- Input parsing and intent recognition
- Error handling and unexpected input
- Quiz scoring accuracy and state persistence
- Fuzzy matching for typos and synonyms
- Web-server communication between frontend/backend

---

## ❗ Known Challenges

- Handling ambiguous user input and correcting it gracefully
- Maintaining clean quiz logic and tracking multiple sessions
- Synchronizing real-time web input with backend state
- Providing meaningful feedback while avoiding chatbot repetition

---

## 🎓 Educational Value

Chaturn is not just a project—it's a learning tool. It helps users explore space, test knowledge, and interact with scientific data in a conversational way.

---

## 💬 Sample Interactions
👤 User: Tell me about Jupiter
🤖 Chaturn: Jupiter is the largest planet in the Solar System... 🌌

👤 User: Start quiz
🤖 Chaturn: Would you like a traditional or personality quiz?

👤 User: Compare Earth and Venus
🤖 Chaturn: Here's a side-by-side look at Earth and Venus:
- Temperature: Earth 15°C | Venus 462°C
- Atmosphere: Nitrogen/Oxygen vs CO₂-dominated

- 
---

## 📈 Future Enhancements

- Add voice input and text-to-speech output
- Support for more languages (e.g., Arabic, French)
- Expand question categories and datasets
- Live astronomical event updates via API

---

> 🌟 _“Bringing the cosmos closer, one conversation at a time.”_  
