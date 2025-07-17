from typing import Dict, List

TRADITIONAL_QUIZ = [
    {
        "question": "What is the closest planet to the Sun?",
        "options": ["Mercury", "Venus", "Earth", "Mars"],
        "answer": "Mercury"
    },
    {
        "question": "Which planet is known as the Red Planet?",
        "options": ["Mars", "Venus", "Jupiter", "Saturn"],
        "answer": "Mars"
    },
    {
        "question": "Which planet has the most moons?",
        "options": ["Jupiter", "Saturn", "Uranus", "Neptune"],
        "answer": "Saturn"
    }
]

PERSONAL_QUIZ = [
    {
        "question": "What's your favorite planet and why?",
        "options": ["Share your thoughts..."],
        "answer": "Any"
    },
    {
        "question": "If you could visit any celestial body, which would it be?",
        "options": ["Share your thoughts..."],
        "answer": "Any"
    },
    {
        "question": "What aspect of space interests you the most?",
        "options": ["Share your thoughts..."],
        "answer": "Any"
    }
]

# Animation configurations
ANIMATIONS = {
    "fade_duration": 100  # milliseconds
}

# Theme configurations
THEMES = {
    "dark": {
        "bg_color": "#1a1a1a",
        "text_color": "#ffffff",
        "button_color": "#2d2d2d"
    },
    "light": {
        "bg_color": "#f0f0f0",
        "text_color": "#000000",
        "button_color": "#e0e0e0"
    }
} 