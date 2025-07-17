# CHATURN - Astronomy Chatbot

CHATURN is an interactive astronomy chatbot with a beautiful GUI, featuring quizzes, facts, and an engaging space theme.

## Features

- Beautiful space-themed GUI with animated background
- Dark and light mode support
- Background space ambience music
- Two types of quizzes:
  - Traditional Quiz: Test your astronomy knowledge
  - Personal Quiz: Share your space interests
- Progress bar for quiz tracking
- Random astronomy facts
- Planet information
- Smooth animations and transitions

## Setup

1. Install the required packages:
```bash
pip install -r requirements.txt
```

2. Generate resources:
```bash
python create_resources.py
```

3. (Optional) Add background music:
- Follow the instructions in `space_ambience.txt`
- Place your `space_ambience.mp3` file in the project directory

## Running the Chatbot

```bash
python astronomy_chatbot.py
```

## Usage

- Type 'help' to see available commands
- Type 'traditional quiz' or 'personal quiz' to start a quiz
- Type 'fact' to get a random astronomy fact
- Ask about any planet (e.g., "Tell me about Mars")
- Use the top bar controls to:
  - Toggle background music
  - Switch between dark and light mode

## Quiz Types

1. Traditional Quiz:
   - Tests your knowledge of astronomy
   - Keeps score of correct answers
   - Shows progress through the quiz

2. Personal Quiz:
   - Asks about your space interests
   - No right or wrong answers
   - Helps personalize the experience

## Contributing

Feel free to contribute to this project by:
1. Adding more quiz questions
2. Expanding the astronomy database
3. Improving animations and transitions
4. Adding new features

## License

This project is open source and available under the MIT License. 