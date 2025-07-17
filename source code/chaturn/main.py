import customtkinter as ctk
import json
import random
from typing import Dict, List, Optional, Tuple
import os
from PIL import Image, ImageDraw, ImageTk
import pygame
import math
import csv
import time

# ANSI Color codes for terminal
class Colors:
    Reset = "\033[0m"
    Black = "\033[30m"
    Red = "\033[31m"
    Green = "\033[32m"
    Yellow = "\033[33m"
    Blue = "\033[34m"
    Purple = "\033[35m"
    Cyan = "\033[36m"
    Blink = "\033[5m"

# Theme configurations
THEMES = {
    "dark": {
        "bg_color": "#1a1a2e",
        "text_color": "#ffffff",
        "button_color": "#2d2d2d",
        "frame_color": "#2d2d2d",
        "accent_color": "#00ff88"
    },
    "light": {
        "bg_color": "#f0f0f0",
        "text_color": "#1a1a2e",
        "button_color": "#e0e0e0",
        "frame_color": "#ffffff",
        "accent_color": "#0066cc"
    }
}

# Animation configurations
ANIMATIONS = {
    "fade_duration": 100  # milliseconds
}

# Configure appearance
ctk.set_appearance_mode("dark")
ctk.set_default_color_theme("blue")

# Constants
class Constants:
    CMD_UNKNOWN = "UNKNOWN"
    CMD_HELP = "HELP"
    CMD_LIST_PLANETS = "LIST_PLANETS"
    CMD_LIST_CATEGORY = "LIST_CATEGORY"
    CMD_RANDOM_FACT = "RANDOM_FACT"
    CMD_START_QUIZ = "START_QUIZ"
    CMD_ANSWER_QUIZ = "ANSWER_QUIZ"
    CMD_ASK_ABOUT = "ASK_ABOUT"
    CMD_COMPARE = "COMPARE"
    CMD_EXIT_QUIZ = "EXIT_QUIZ"
    CMD_SKIP_QUESTION = "SKIP_QUESTION"
    CMD_GREETINGS = "GREETINGS"

    # Word lists
    help_words = ["help", "commands", "guide", "instructions"]
    list_words = ["list", "show", "display", "name", "what"]
    fact_words = [
        "fact", "facts", "trivia", "interesting", "random", "cool", "fun",
        "tell me a fact", "give me a fact", "share a fact", "tell me something",
        "surprise me", "did you know"
    ]
    quiz_words = ["quiz", "trivia", "test", "challenge", "game", "start quiz", "quiz me"]
    compare_words = [
        "compare", "difference", "versus", "vs", "between", "against", "and",
        "or", "difference between", "how does", "how do", "what is the difference"
    ]
    exit_words = ["exit", "quit", "stop", "end", "exit quiz", "stop quiz", "end quiz"]
    greeting_words = ["hello", "hi", "hey", "greetings", "yo"]
    planets = ["mars", "jupiter", "saturn", "uranus", "neptune", "venus", "mercury", "earth", "pluto"]

    categories = [
        "stars", "constellations", "moons", "dwarf planets", "galaxies",
        "black holes", "asteroids", "comets", "nebulae", "star systems", "exoplanets"
    ]

    celestial_objects = [
        "stars", "planets", "moons", "dwarf planets", "galaxies",
        "comets", "asteroids", "nebulae"
    ]

# Quiz Data
TRADITIONAL_QUIZ = [
    {
        "question": "What is the name of our galaxy?",
        "options": ["Butterfly Galaxy", "Milky Way Galaxy", "Spiral Galaxy", "Andromeda Galaxy"],
        "answer": "Milky Way Galaxy/Milky Way/Our Galaxy"
    },
    {
        "question": "What is the smallest planet in our solar system?",
        "options": ["Mercury", "Mars", "Pluto", "Venus"],
        "answer": "Mercury/Smallest Planet/First Planet"
    },
    {
        "question": "Which planet is known as the Red Planet?",
        "options": ["Jupiter", "Mars", "Venus", "Mercury"],
        "answer": "Mars/Red Planet/Fourth Planet"
    },
    {
        "question": "What is the largest planet in our solar system?",
        "options": ["Neptune", "Jupiter", "Saturn", "Uranus"],
        "answer": "Jupiter/Largest Planet/Gas Giant"
    },
    {
        "question": "What is the approximate distance of Earth from the Sun?",
        "options": ["149.6 million km", "200 million km", "100 million km", "300 million km"],
        "answer": "149.6 million km/150 million km/1 AU"
    },
    {
        "question": "Which planet is known for its beautiful rings?",
        "options": ["Jupiter", "Mars", "Saturn", "Uranus"],
        "answer": "Saturn/Ringed Planet/Sixth Planet"
    },
    {
        "question": "What is the average surface temperature on Venus?",
        "options": ["462°C", "100°C", "200°C", "300°C"],
        "answer": "462/460/462 degrees"
    }
]

PERSONAL_QUIZ = [
    {
        "question": "What is your favorite planet in our solar system?",
        "options": ["Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune"],
        "answer": None
    },
    {
        "question": "Which celestial phenomenon would you most like to see?",
        "options": ["Solar Eclipse", "Aurora Borealis", "Meteor Shower", "Supernova"],
        "answer": None
    },
    {
        "question": "If you could visit any place in space, where would you go?",
        "options": ["Moon", "Mars", "Jupiter's Moons", "Saturn's Rings"],
        "answer": None
    },
    {
        "question": "Which space mission interests you the most?",
        "options": ["Moon Landing", "Mars Colonization", "Deep Space Exploration", "Space Tourism"],
        "answer": None
    },
    {
        "question": "What aspect of astronomy fascinates you most?",
        "options": ["Black Holes", "Alien Life", "Galaxy Formation", "Star Life Cycles"],
        "answer": None
    }
]

class InputParser:
    @staticmethod
    def parse_input(input_str: str, is_quiz_active: bool = False) -> str:
        words = input_str.lower().split()
        
        if is_quiz_active:
            return InputParser.parse_quiz_mode(words, input_str)
        else:
            return InputParser.parse_regular_mode(words, input_str)

    @staticmethod
    def contains_any(words: List[str], target_list: List[str]) -> bool:
        return any(word in target_list for word in words)

    @staticmethod
    def matches_greetings(words: List[str]) -> bool:
        return InputParser.contains_any(words, Constants.greeting_words)

    @staticmethod
    def matches_help(words: List[str]) -> bool:
        return InputParser.contains_any(words, Constants.help_words)

    @staticmethod
    def matches_list_planets(words: List[str]) -> bool:
        return (InputParser.contains_any(words, Constants.list_words) and 
                "planets" in words)

    @staticmethod
    def matches_random_fact(words: List[str]) -> bool:
        return InputParser.contains_any(words, Constants.fact_words)

    @staticmethod
    def matches_quiz(words: List[str]) -> bool:
        return InputParser.contains_any(words, Constants.quiz_words)

    @staticmethod
    def matches_compare(input_str: str) -> bool:
        return (InputParser.contains_any(input_str.split(), Constants.compare_words) and
                "and" in input_str.lower())

    @staticmethod
    def matches_category(words: List[str]) -> bool:
        def category_matches(cat: str) -> bool:
            return cat in words or cat.replace(" ", "") in words

        return (InputParser.contains_any(words, Constants.list_words) and
                any(category_matches(cat) for cat in Constants.categories))

    @staticmethod
    def matches_planet(words: List[str]) -> bool:
        return any(planet in words for planet in Constants.planets)

    @staticmethod
    def extract_compare_topics(input_str: str) -> Optional[Tuple[str, str]]:
        input_str = input_str.lower()
        
        # Find the position of "and" or similar words
        for word in ["and", "vs", "versus"]:
            if word in input_str:
                parts = input_str.split(word)
                if len(parts) == 2:
                    topic1 = InputParser.extract_topic(parts[0])
                    topic2 = InputParser.extract_topic(parts[1])
                    if topic1 and topic2:
                        return (topic1, topic2)
        
        return None

    @staticmethod
    def extract_topic(input_str: str) -> str:
        def remove_prefix(text: str, remaining_prefixes: List[str]) -> str:
            for prefix in remaining_prefixes:
                if text.startswith(prefix):
                    return text[len(prefix):].strip()
            return text.strip()

        def remove_filler_words(text: str, fillers: List[str]) -> str:
            words = text.split()
            return " ".join(word for word in words if word not in fillers)

        # Remove common prefixes
        prefixes = ["tell me about", "what about", "how about", "compare", "and"]
        text = remove_prefix(input_str.lower(), prefixes)
        
        # Remove filler words
        fillers = ["the", "a", "an", "this", "that", "these", "those"]
        text = remove_filler_words(text, fillers)
        
        return text.strip()

    @staticmethod
    def parse_quiz_mode(words: List[str], original_input: str) -> Tuple[str, str, str]:
        def contains_exit_word(ws: List[str]) -> bool:
            return any(word in Constants.exit_words for word in ws)

        if contains_exit_word(words):
            return Constants.CMD_EXIT_QUIZ, "", ""
        elif "skip" in words:
            return Constants.CMD_SKIP_QUESTION, "", ""
        else:
            return Constants.CMD_ANSWER_QUIZ, original_input, ""

    @staticmethod
    def parse_regular_mode(words: List[str], original_input: str) -> Tuple[str, str, str]:
        # First check for quiz commands
        if InputParser.matches_quiz(words):
            return Constants.CMD_START_QUIZ, "", ""
            
        # Then check for casual conversation inputs
        if InputParser.matches_greetings(words):
            return Constants.CMD_GREETINGS, "", ""
            
        # Then check for help command
        if InputParser.matches_help(words):
            return Constants.CMD_HELP, "", ""
            
        # Check for random fact request
        if InputParser.matches_random_fact(words):
            return Constants.CMD_RANDOM_FACT, "", ""
            
        # Check for list planets command
        if InputParser.matches_list_planets(words):
            return Constants.CMD_LIST_PLANETS, "", ""
            
        # Check for list category command
        if InputParser.matches_category(words):
            def find_category() -> str:
                for category in Constants.categories:
                    if category in original_input.lower() or category.replace(" ", "") in original_input.lower():
                        return category
                return ""
            return Constants.CMD_LIST_CATEGORY, find_category(), ""
            
        # Check for planet information request
        if InputParser.matches_planet(words):
            def find_planet() -> str:
                for planet in Constants.planets:
                    if planet in original_input.lower():
                        return planet
                return ""
            return Constants.CMD_ASK_ABOUT, find_planet(), ""
            
        # Check for comparison request
        if InputParser.matches_compare(original_input):
            topics = InputParser.extract_compare_topics(original_input)
            if topics:
                return Constants.CMD_COMPARE, topics[0], topics[1]
            
        # If no other command matches, return unknown
        return Constants.CMD_UNKNOWN, "", ""

class DataLoader:
    @staticmethod
    def load_astronomy_data(filename: str) -> Dict[str, Dict[str, str]]:
        try:
            with open(filename, 'r') as file:
                content = file.read()
                
            def parse_objects(json_str: str) -> List[Dict[str, str]]:
                objects_str = json_str.strip().strip('[]').strip()
                object_strings = split_objects(objects_str)
                return [parse_object(obj) for obj in object_strings]
            
            def split_objects(s: str) -> List[str]:
                objects = []
                current_object = []
                brace_count = 0
                in_quotes = False
                
                for char in s:
                    current_object.append(char)
                    if char == '"' and current_object[-2] != '\\':
                        in_quotes = not in_quotes
                    elif not in_quotes:
                        if char == '{':
                            brace_count += 1
                        elif char == '}':
                            brace_count -= 1
                            if brace_count == 0:
                                objects.append(''.join(current_object))
                                current_object = []
                
                return [obj.strip() for obj in objects if obj.strip()]
            
            def parse_object(obj_str: str) -> Dict[str, str]:
                clean_str = obj_str.strip().strip('{}').strip()
                pairs = split_key_value_pairs(clean_str)
                
                result = {}
                for pair in pairs:
                    key, value = pair.split(':', 1)
                    clean_key = key.strip().strip('"')
                    clean_value = value.strip().strip('"').rstrip(',')
                    result[clean_key] = clean_value
                return result
            
            def split_key_value_pairs(s: str) -> List[str]:
                pairs = []
                current_pair = []
                in_quotes = False
                depth = 0
                
                for char in s:
                    if char == '"' and current_pair[-1:] != ['\\']:
                        in_quotes = not in_quotes
                    elif not in_quotes:
                        if char in '{[':
                            depth += 1
                        elif char in ']}':
                            depth -= 1
                        elif char == ',' and depth == 0:
                            pairs.append(''.join(current_pair))
                            current_pair = []
                            continue
                    current_pair.append(char)
                
                if current_pair:
                    pairs.append(''.join(current_pair))
                return [p.strip() for p in pairs if p.strip()]
            
            objects = parse_objects(content)
            return {obj.get('name', 'unknown').lower(): obj for obj in objects}
            
        except Exception as e:
            print(f"{Colors.Red}Error loading astronomy data: {str(e)}{Colors.Reset}")
            return {}

    @staticmethod
    def load_space_objects_data(filename: str) -> Dict[str, Dict[str, str]]:
        try:
            with open(filename, 'r') as file:
                lines = file.readlines()
            
            if not lines:
                return {}
            
            headers = [h.strip() for h in lines[0].split(',')]
            data = {}
            
            for line in lines[1:]:
                try:
                    values = next(csv.reader([line]))
                    if len(values) >= len(headers):
                        object_data = {
                            headers[i].strip(): values[i].strip().strip('"').strip()
                            for i in range(len(headers))
                        }
                        name = object_data.get('name', 'unknown').lower()
                        if name != 'unknown':
                            data[name] = object_data
                except Exception as e:
                    print(f"{Colors.Red}Warning: Skipping malformed line in CSV: {line}{Colors.Reset}")
            
            return data
            
        except Exception as e:
            print(f"{Colors.Red}Error loading space objects data: {str(e)}{Colors.Reset}")
            return {}

    @staticmethod
    def merge_data(json_data: Dict[str, Dict[str, str]], 
                   csv_data: Dict[str, Dict[str, str]]) -> Dict[str, Dict[str, str]]:
        all_keys = set(json_data.keys()) | set(csv_data.keys())
        merged_data = {}
        
        for key in all_keys:
            json_entry = json_data.get(key, {})
            csv_entry = csv_data.get(key, {})
            # CSV data takes precedence over JSON data for the same fields
            merged_data[key] = {**json_entry, **csv_entry}
        
        return merged_data

    # Class variables for lazy loading
    _astronomy_data = None
    _space_objects_data = None

    @classmethod
    def astronomy_data(cls) -> Dict[str, Dict[str, str]]:
        if cls._astronomy_data is None:
            json_data = cls.load_astronomy_data("astronomy.json")
            csv_data = cls.load_space_objects_data("space_objects.csv")
            cls._astronomy_data = cls.merge_data(json_data, csv_data)
        return cls._astronomy_data

    @classmethod
    def space_objects_data(cls) -> Dict[str, Dict[str, str]]:
        if cls._space_objects_data is None:
            cls._space_objects_data = cls.load_space_objects_data("space_objects.csv")
        return cls._space_objects_data 

class AnalyticsImpl:
    def __init__(self):
        self.command_counts = {}
        self.total_interactions = 0
    
    def log_interaction(self, command: str) -> None:
        self.total_interactions += 1
        self.command_counts[command] = self.command_counts.get(command, 0) + 1
    
    def get_total_interactions(self) -> int:
        return self.total_interactions
    
    def get_most_frequent_command(self) -> Optional[Tuple[str, int]]:
        if not self.command_counts:
            return None
        return max(self.command_counts.items(), key=lambda x: x[1])
    
    def get_command_statistics(self) -> Dict[str, int]:
        return self.command_counts.copy()

class QuizManagerImpl:
    def __init__(self):
        self.current_quiz = []
        self.current_question_idx = 0
        self.quiz_type = ""
        self.score = 0
        self.total_questions = 0
        self.user_name = "Space Explorer"
        self.user_preferences = {}
        self.is_quiz_active = False
        self.waiting_for_quiz_selection = False
        self.hints_used = 0
        self.wrong_answers = []
        self.response_times = []
        self.start_time = None

    def levenshtein_distance(self, s1: str, s2: str) -> int:
        """Calculate the Levenshtein distance between two strings."""
        if len(s1) < len(s2):
            return self.levenshtein_distance(s2, s1)

        if len(s2) == 0:
            return len(s1)

        previous_row = range(len(s2) + 1)
        for i, c1 in enumerate(s1):
            current_row = [i + 1]
            for j, c2 in enumerate(s2):
                insertions = previous_row[j + 1] + 1
                deletions = current_row[j] + 1
                substitutions = previous_row[j] + (c1 != c2)
                current_row.append(min(insertions, deletions, substitutions))
            previous_row = current_row

        return previous_row[-1]

    def string_similarity(self, s1: str, s2: str) -> float:
        """Calculate string similarity as a ratio between 0 and 1."""
        # Clean and normalize strings
        s1 = ''.join(c.lower() for c in s1 if c.isalnum() or c.isspace())
        s2 = ''.join(c.lower() for c in s2 if c.isalnum() or c.isspace())
        
        # Handle empty strings
        if not s1 and not s2:
            return 1.0
        if not s1 or not s2:
            return 0.0
            
        # Calculate Levenshtein distance
        distance = self.levenshtein_distance(s1, s2)
        max_len = max(len(s1), len(s2))
        
        # Calculate similarity ratio
        base_similarity = 1 - (distance / max_len)
        
        # Boost score for partial matches
        if s1 in s2 or s2 in s1:
            base_similarity += 0.1
            
        # Boost score for same first letter
        if s1[0] == s2[0]:
            base_similarity += 0.05
            
        return min(1.0, base_similarity)

    def is_answer_similar(self, user_answer: str, correct_answer: str, threshold: float = 0.85) -> bool:
        """Check if the user's answer is similar enough to the correct answer."""
        # Clean and normalize both answers
        user_answer = user_answer.lower().strip()
        correct_answer = correct_answer.lower().strip()
        
        # Direct match
        if user_answer == correct_answer:
            return True
            
        # Handle numeric answers with units
        if any(c.isdigit() for c in correct_answer):
            # Extract numbers and units
            def extract_number_and_unit(text):
                import re
                number_match = re.search(r'(\d+\.?\d*)', text)
                unit_match = re.search(r'([a-zA-Z°]+)', text)
                number = float(number_match.group(1)) if number_match else None
                unit = unit_match.group(1).lower() if unit_match else None
                return number, unit
                
            user_num, user_unit = extract_number_and_unit(user_answer)
            correct_num, correct_unit = extract_number_and_unit(correct_answer)
            
            if user_num is not None and correct_num is not None:
                # Check if numbers are close (within 5% tolerance)
                number_match = abs(user_num - correct_num) / correct_num < 0.05
                # Check if units match or are compatible
                unit_match = True
                if user_unit and correct_unit:
                    unit_match = self.are_units_compatible(user_unit, correct_unit)
                return number_match and unit_match

        # Handle multiple acceptable answers
        if '/' in correct_answer:
            return any(self.is_answer_similar(user_answer, alt.strip(), threshold) 
                      for alt in correct_answer.split('/'))

        # Split into words and check each word
        user_words = user_answer.split()
        correct_words = correct_answer.split()
        
        # Single word answers
        if len(user_words) == 1 and len(correct_words) == 1:
            return self.string_similarity(user_answer, correct_answer) > threshold
        
        # Multi-word answers
        matches = 0
        total_weight = 0
        
        # Create word pairs for comparison
        for u_word in user_words:
            best_match = 0
            for c_word in correct_words:
                similarity = self.string_similarity(u_word, c_word)
                best_match = max(best_match, similarity)
            matches += best_match
            total_weight += 1
            
        # Calculate weighted average similarity
        if total_weight > 0:
            avg_similarity = matches / total_weight
            return avg_similarity > threshold
            
        return False

    def are_units_compatible(self, unit1: str, unit2: str) -> bool:
        """Check if two units are compatible."""
        # Define unit compatibility groups
        unit_groups = {
            'distance': {'km', 'kilometers', 'kilometer', 'kms', 'au', 'astronomical units', 'light years', 'ly'},
            'temperature': {'c', 'celsius', '°c', 'k', 'kelvin', '°k', 'f', 'fahrenheit', '°f'},
            'time': {'s', 'seconds', 'sec', 'min', 'minutes', 'h', 'hours', 'hr', 'hrs', 'days', 'years', 'yr', 'yrs'},
            'mass': {'kg', 'kilograms', 'g', 'grams', 'tons', 'tonnes'},
        }
        
        # Normalize units
        unit1 = unit1.lower().strip('.')
        unit2 = unit2.lower().strip('.')
        
        # Direct match
        if unit1 == unit2:
            return True
            
        # Check if units belong to the same group
        for group in unit_groups.values():
            if unit1 in group and unit2 in group:
                return True
                
        return False

    def get_hint(self, question: dict) -> str:
        """Generate a hint for the current question."""
        if "hint" in question:
            return question["hint"]
            
        answer = question["answer"].split('/')[0].lower()
        
        # For multiple choice questions
        if question["options"]:
            # Eliminate two wrong options
            wrong_options = [opt for opt in question["options"] if opt.lower() not in answer.lower()]
            eliminated = random.sample(wrong_options, min(2, len(wrong_options)))
            return f"Hint: These options are incorrect: {', '.join(eliminated)}"
            
        # For text answers
        if "planet" in question["question"].lower():
            return "Hint: This is one of the planets in our solar system."
        elif "temperature" in question["question"].lower():
            return "Hint: Think about the planet's distance from the Sun."
        elif "largest" in question["question"].lower():
            return "Hint: Consider the gas giants."
        elif "smallest" in question["question"].lower():
            return "Hint: Look at the inner planets."
        elif "galaxy" in question["question"].lower():
            return "Hint: We live in this galaxy."
        else:
            # Generic hint - reveal first letter
            return f"Hint: The answer starts with '{answer[0].upper()}'"

    def analyze_performance(self) -> str:
        """Analyze quiz performance and provide detailed feedback."""
        if not self.total_questions:
            return "No quiz data available."
            
        accuracy = (self.score / self.total_questions) * 100
        avg_time = sum(self.response_times) / len(self.response_times) if self.response_times else 0
        
        # Create performance bars
        accuracy_bar = "█" * int(accuracy/5) + "░" * (20 - int(accuracy/5))
        speed_rating = "Fast" if avg_time < 15 else "Average" if avg_time < 30 else "Take your time"
        
        # Analyze wrong answers for pattern
        topic_mistakes = {}
        for q in self.wrong_answers:
            topic = "General"
            if "planet" in q["question"].lower():
                topic = "Planets"
            elif "galaxy" in q["question"].lower():
                topic = "Galaxies"
            elif "temperature" in q["question"].lower():
                topic = "Planetary Conditions"
            elif "distance" in q["question"].lower():
                topic = "Astronomical Distances"
                
            topic_mistakes[topic] = topic_mistakes.get(topic, 0) + 1
        
        # Generate improvement suggestions
        suggestions = []
        if accuracy < 60:
            suggestions.append("• Review the basic astronomy concepts")
        if self.hints_used > self.total_questions / 2:
            suggestions.append("• Try to answer without hints to improve retention")
        if avg_time > 30:
            suggestions.append("• Work on quick recall of astronomy facts")
        
        # Find strongest and weakest topics
        if topic_mistakes:
            worst_topic = max(topic_mistakes.items(), key=lambda x: x[1])[0]
            suggestions.append(f"• Focus on studying {worst_topic}")
        
        analysis = f"""📊 Performance Analysis:

Accuracy: [{accuracy_bar}] {accuracy:.1f}%
Response Time: {avg_time:.1f} seconds (Rating: {speed_rating})
Hints Used: {self.hints_used} out of {self.total_questions} questions

🎯 Topic Performance:"""
        
        # Add topic breakdown if there are mistakes
        if topic_mistakes:
            for topic, count in topic_mistakes.items():
                topic_accuracy = 100 * (1 - count/self.total_questions)
                analysis += f"\n• {topic}: {topic_accuracy:.1f}% accuracy"
        else:
            analysis += "\n• Perfect score across all topics!"
        
        if suggestions:
            analysis += "\n\n💡 Suggestions for Improvement:\n" + "\n".join(suggestions)
        
        return analysis

    def handle_message(self, message: str) -> str:
        message = message.lower().strip()
        
        # Handle quiz selection
        if self.waiting_for_quiz_selection:
            if message in ["1", "traditional"]:
                self.start_time = time.time()  # Start timing
                return self.start_quiz("traditional")
            elif message in ["2", "personal"]:
                return self.start_quiz("personal")
            else:
                return "Please select a valid option: Type '1' or 'traditional' for Traditional Quiz, '2' or 'personal' for Personal Quiz."
        
        # Handle active quiz
        if self.is_quiz_active:
            # Handle hint request
            if message == "hint":
                self.hints_used += 1
                return self.get_hint(self.current_quiz[self.current_question_idx])
            
            # Handle quiz exit
            if message in ["exit", "quit", "stop"]:
                self.is_quiz_active = False
                if self.quiz_type == "traditional":
                    return f"""Quiz ended! Final Results:

{self.analyze_performance()}"""
                else:
                    return "Thanks for sharing your preferences! I'll remember them for our future chats."
            
            # Handle skip
            if message == "skip":
                if self.current_question_idx < len(self.current_quiz) - 1:
                    self.current_question_idx += 1
                    return self.format_current_question()
                else:
                    self.is_quiz_active = False
                    if self.quiz_type == "traditional":
                        return f"""Quiz completed! Final Results:

{self.analyze_performance()}"""
                    else:
                        return "Thanks for sharing your preferences! I'll remember them for our future chats."
            
            # Process answer
            if self.quiz_type == "traditional":
                # Record response time
                if self.start_time:
                    self.response_times.append(time.time() - self.start_time)
                    self.start_time = time.time()  # Reset for next question
                
                correct = self.check_answer(message)
                if correct:
                    self.score += 1
                    response = "✨ Correct! "
                else:
                    correct_answer = self.current_quiz[self.current_question_idx]['answer'].split('/')[0]
                    response = f"❌ Not quite. The correct answer was: {correct_answer}. "
                    self.wrong_answers.append(self.current_quiz[self.current_question_idx])
            else:
                # For personal quiz, store the preference
                self.user_preferences[self.current_quiz[self.current_question_idx]["question"]] = message
                response = "🌟 Thanks for sharing! "
            
            # Move to next question or end quiz
            if self.current_question_idx < len(self.current_quiz) - 1:
                self.current_question_idx += 1
                return response + "\n\n" + self.format_current_question()
            else:
                self.is_quiz_active = False
                if self.quiz_type == "traditional":
                    return response + f"""\n\nQuiz completed! Final Results:

{self.analyze_performance()}"""
                else:
                    return response + "\n\nThanks for sharing your preferences! I'll remember them for our future chats."
        
        return "Something went wrong with the quiz. Type 'quiz' to start over."

    def check_answer(self, answer: str) -> bool:
        """Check if the answer is correct for traditional quiz."""
        current_question = self.current_quiz[self.current_question_idx]
        
        # If it's a multiple choice question
        if current_question["options"]:
            # Try to match the answer with option number
            try:
                if answer.isdigit():
                    idx = int(answer) - 1
                    if 0 <= idx < len(current_question["options"]):
                        answer = current_question["options"][idx]
            except:
                pass
            
            # Check against all acceptable answers
            acceptable_answers = current_question["answer"].lower().split('/')
            
            # Try exact matches first
            for acc_answer in acceptable_answers:
                if answer.lower().strip() == acc_answer.strip():
                    return True
            
            # Then try fuzzy matching with different thresholds
            thresholds = [0.95, 0.90, 0.85]  # Try stricter thresholds first
            for threshold in thresholds:
                for acc_answer in acceptable_answers:
                    if self.is_answer_similar(answer, acc_answer, threshold):
                        return True
            
            # Finally, check if the answer is similar to any of the options
            for option in current_question["options"]:
                if self.is_answer_similar(answer, option, 0.85):
                    # Double check if this option is a correct answer
                    if any(self.is_answer_similar(option, acc, 0.85) for acc in acceptable_answers):
                        return True
        
        return False

    def format_current_question(self) -> str:
        """Format the current question with options if available."""
        if not self.current_quiz or self.current_question_idx >= len(self.current_quiz):
            return "No questions available."
        
        question = self.current_quiz[self.current_question_idx]
        
        # Create progress bar
        total_width = 20
        progress = self.current_question_idx + 1
        total = len(self.current_quiz)
        filled = int((progress / total) * total_width)
        progress_bar = "█" * filled + "░" * (total_width - filled)
        
        # Format header with progress information
        header = f"Question {progress}/{total}\n"
        header += f"Progress: [{progress_bar}] {int((progress/total)*100)}%\n"
        if self.quiz_type == "traditional":
            header += f"Score: {self.score}/{self.current_question_idx}\n"
            header += f"Hints Available: Type 'hint' for help\n"
        header += "\n"
        
        # Format question and options
        formatted = f"{header}{question['question']}\n"
        
        if question["options"]:
            formatted += "\nOptions:\n"
            for i, option in enumerate(question["options"], 1):
                formatted += f"{i}. {option}\n"
            formatted += "\nType the number or the answer text. Type 'hint' for help."
        
        return formatted

    def start_quiz_selection(self) -> str:
        """Show quiz selection options."""
        self.waiting_for_quiz_selection = True
        return """Please choose the type of quiz you'd like to take:

1️⃣ Traditional Quiz
   • Test your astronomy knowledge
   • Get scored on your answers
   • Learn interesting facts

2️⃣ Personal Quiz
   • Share your space preferences
   • Help me understand your interests
   • No right or wrong answers

Type '1' or 'traditional' for Traditional Quiz
Type '2' or 'personal' for Personal Quiz"""

    def start_quiz(self, quiz_type: str) -> str:
        """Start the selected quiz type."""
        self.waiting_for_quiz_selection = False
        self.is_quiz_active = True
        self.quiz_type = quiz_type
        self.current_quiz = TRADITIONAL_QUIZ if quiz_type == "traditional" else PERSONAL_QUIZ
        self.current_question_idx = 0
        self.score = 0
        self.total_questions = len(self.current_quiz)
        
        intro = ("Let's test your astronomy knowledge!" if quiz_type == "traditional" 
                else "I'd love to learn about your space interests!")
        
        return intro + "\n\n" + self.format_current_question()

class WelcomePage(ctk.CTkToplevel):
    def __init__(self, parent, proceed_callback):
        super().__init__(parent)
        
        # Configure window
        self.title("Welcome to CHATURN")
        self.geometry("600x500")
        self.resizable(False, False)
        
        # Store callback
        self.proceed_callback = proceed_callback
        
        # Make sure this window stays on top and centered
        self.transient(parent)
        self.grab_set()
        self.protocol("WM_DELETE_WINDOW", self.on_proceed)
        
        # Create content
        self.create_welcome_content()
        
        # Center window on screen
        self.update_idletasks()
        width = self.winfo_width()
        height = self.winfo_height()
        x = (self.winfo_screenwidth() // 2) - (width // 2)
        y = (self.winfo_screenheight() // 2) - (height // 2)
        self.geometry(f"{width}x{height}+{x}+{y}")
    
    def create_welcome_content(self):
        # Background frame
        main_frame = ctk.CTkFrame(self)
        main_frame.pack(fill="both", expand=True, padx=20, pady=20)
        
        # Welcome text
        welcome_label = ctk.CTkLabel(
            main_frame,
            text="Welcome to CHATURN",
            font=("Helvetica", 24, "bold")
        )
        welcome_label.pack(pady=(20, 10))
        
        # Description
        desc_label = ctk.CTkLabel(
            main_frame,
            text="Your personal guide to the cosmos",
            font=("Helvetica", 16)
        )
        desc_label.pack(pady=(0, 20))
        
        # Name entry
        name_frame = ctk.CTkFrame(main_frame)
        name_frame.pack(fill="x", padx=50, pady=(0, 20))
        
        name_label = ctk.CTkLabel(
            name_frame,
            text="What's your name, space explorer?",
            font=("Helvetica", 14)
        )
        name_label.pack(pady=(10, 5))
        
        self.name_entry = ctk.CTkEntry(
            name_frame,
            placeholder_text="Enter your name",
            width=200
        )
        self.name_entry.pack(pady=(0, 10))
        
        # Proceed button
        proceed_button = ctk.CTkButton(
            main_frame,
            text="Start Exploring",
            command=self.on_proceed,
            width=200
        )
        proceed_button.pack(pady=20)
    
    def on_proceed(self):
        """Close welcome screen and proceed to main app"""
        name = self.name_entry.get().strip()
        if not name:
            name = "Space Explorer"
        self.grab_release()
        self.destroy()
        self.proceed_callback(name)  # Pass the name to the callback

class AstronomyChatbotGUI(ctk.CTk):
    def __init__(self):
        super().__init__()
        
        # Initialize components
        self.quiz_manager = QuizManagerImpl()
        self.analytics = AnalyticsImpl()
        self.current_theme = "dark"
        self.music_playing = False
        
        # Configure window
        self.title("CHATURN - Astronomy Chatbot")
        self.geometry("1000x700")  # Larger window for better visibility
        self.minsize(800, 600)     # Minimum window size
        
        # Initialize pygame for music
        pygame.mixer.init()
        
        # Show welcome page
        self.withdraw()  # Hide main window initially
        self.welcome = WelcomePage(self, self.after_welcome)
    
    def set_user_name(self, name: str):
        """Set the user name and update the window title."""
        self.quiz_manager.user_name = name  # Set the name in quiz manager
        self.title(f"CHATURN - Welcome, {name}!")  # Update window title
    
    def after_welcome(self, name: str):
        self.set_user_name(name)
        self.deiconify()  # Show main window
        self.create_gui()
        self.setup_music()
        
        # Add initial bot message
        welcome_msg = (
            f"Hello {name}! I'm CHATURN, your astronomy companion. "
            "I can help you learn about planets, stars, and the mysteries of space. "
            "Type 'help' to see what I can do!"
        )
        self.add_bot_message(welcome_msg)
    
    def setup_music(self):
        try:
            pygame.mixer.music.load("Soft Music For Studying Concentration Short 10 Minutes.mp3")
            pygame.mixer.music.set_volume(0.5)
        except Exception as e:
            print(f"Could not load music: {e}")
    
    def toggle_music(self):
        try:
            if self.music_playing:
                pygame.mixer.music.pause()
                self.music_btn.configure(text="🔇 Music Off")
            else:
                pygame.mixer.music.play(-1)  # -1 means loop indefinitely
                self.music_btn.configure(text="🔊 Music On")
            self.music_playing = not self.music_playing
        except Exception as e:
            print(f"Error toggling music: {e}")
    
    def toggle_theme(self):
        self.current_theme = "light" if self.current_theme == "dark" else "dark"
        self.apply_theme()
    
    def apply_theme(self):
        theme = THEMES[self.current_theme]
        self.configure(fg_color=theme["bg_color"])
        self.chat_frame.configure(fg_color=theme["frame_color"])
        self.theme_btn.configure(text="🌙 Dark" if self.current_theme == "light" else "☀️ Light")
        
        # Update all message frames
        for child in self.chat_frame.winfo_children():
            if isinstance(child, ctk.CTkFrame):
                if "👤" in child.winfo_children()[-1]._text:  # User message
                    child.configure(fg_color=theme["accent_color"])
                else:  # Bot message
                    child.configure(fg_color=theme["frame_color"])
    
    def create_gui(self):
        # Create main container with gradient effect
        self.main_container = ctk.CTkFrame(self)
        self.main_container.pack(fill="both", expand=True, padx=20, pady=20)
        
        # Create header frame
        self.create_header()
        
        # Create chat frame with custom styling
        self.chat_frame = ctk.CTkScrollableFrame(
            self.main_container,
            corner_radius=15,
            border_width=1
        )
        self.chat_frame.pack(fill="both", expand=True, padx=10, pady=(0, 10))
        
        # Create suggestion buttons
        self.create_suggestion_buttons()
        
        # Create input area
        self.create_input_area()
        
        # Create status bar
        self.create_status_bar()
    
    def create_header(self):
        header = ctk.CTkFrame(self.main_container, height=60)
        header.pack(fill="x", padx=10, pady=(0, 10))
        
        # Logo/Title
        title_frame = ctk.CTkFrame(header, fg_color="transparent")
        title_frame.pack(side="left", padx=10)
        
        title = ctk.CTkLabel(
            title_frame,
            text="🌌 CHATURN",
            font=("Helvetica", 24, "bold")
        )
        title.pack(side="left")
        
        subtitle = ctk.CTkLabel(
            title_frame,
            text="Your Cosmic Companion",
            font=("Helvetica", 12)
        )
        subtitle.pack(side="left", padx=10)
        
        # Control buttons
        controls = ctk.CTkFrame(header, fg_color="transparent")
        controls.pack(side="right", padx=10)
        
        self.theme_btn = ctk.CTkButton(
            controls,
            text="☀️ Light",
            width=100,
            height=32,
            corner_radius=16,
            command=self.toggle_theme
        )
        self.theme_btn.pack(side="left", padx=5)
        
        self.music_btn = ctk.CTkButton(
            controls,
            text="🔇 Music Off",
            width=100,
            height=32,
            corner_radius=16,
            command=self.toggle_music
        )
        self.music_btn.pack(side="left", padx=5)
    
    def create_suggestion_buttons(self):
        suggestions = ctk.CTkFrame(self.main_container, fg_color="transparent")
        suggestions.pack(fill="x", padx=10, pady=5)
        
        suggestions_label = ctk.CTkLabel(
            suggestions,
            text="Quick Actions:",
            font=("Helvetica", 12, "bold")
        )
        suggestions_label.pack(side="left", padx=5)
        
        # Common actions
        actions = [
            ("🌍 Planets", "list planets"),
            ("❓ Help", "help"),
            ("🎲 Random Fact", "random fact"),
            ("🎮 Quiz", "start quiz")
        ]
        
        for text, command in actions:
            btn = ctk.CTkButton(
                suggestions,
                text=text,
                width=100,
                height=28,
                corner_radius=14,
                command=lambda cmd=command: self.quick_action(cmd)
            )
            btn.pack(side="left", padx=5)
    
    def create_input_area(self):
        input_frame = ctk.CTkFrame(self.main_container)
        input_frame.pack(fill="x", padx=10, pady=(0, 10))
        
        # Create input field with placeholder and styling
        self.input_field = ctk.CTkEntry(
            input_frame,
            placeholder_text="Ask me about the cosmos...",
            height=40,
            font=("Helvetica", 14),
            corner_radius=20
        )
        self.input_field.pack(side="left", fill="x", expand=True, padx=(0, 10))
        
        # Create send button with icon
        send_btn = ctk.CTkButton(
            input_frame,
            text="Send 🚀",
            width=100,
            height=40,
            corner_radius=20,
            command=self.send_message
        )
        send_btn.pack(side="right")
        
        # Bind Enter key to send message
        self.input_field.bind("<Return>", lambda e: self.send_message())
    
    def create_status_bar(self):
        status_bar = ctk.CTkFrame(self.main_container, height=25, fg_color="transparent")
        status_bar.pack(fill="x", padx=10)
        
        # Show total interactions
        self.status_label = ctk.CTkLabel(
            status_bar,
            text=f"Total Interactions: {self.analytics.get_total_interactions()}",
            font=("Helvetica", 10)
        )
        self.status_label.pack(side="left")
    
    def quick_action(self, command: str):
        """Handle quick action button clicks"""
        self.input_field.delete(0, "end")
        self.input_field.insert(0, command)
        self.send_message()
    
    def add_bot_message(self, message: str):
        # Create message container
        msg_frame = ctk.CTkFrame(
            self.chat_frame,
            fg_color=THEMES[self.current_theme]["frame_color"],
            corner_radius=10
        )
        msg_frame.pack(anchor="w", pady=5, padx=5, fill="x")
        
        # Bot icon
        icon = ctk.CTkLabel(
            msg_frame,
            text="🤖",
            font=("Helvetica", 20)
        )
        icon.pack(side="left", padx=5, pady=5)
        
        # Message text
        label = ctk.CTkLabel(
            msg_frame,
            text=message,
            wraplength=600,
            justify="left",
            font=("Helvetica", 12)
        )
        label.pack(side="left", pady=10, padx=5, fill="x", expand=True)
        
        # Scroll to bottom
        self.chat_frame._parent_canvas.yview_moveto(1.0)
    
    def add_user_message(self, message: str):
        # Create message container
        msg_frame = ctk.CTkFrame(
            self.chat_frame,
            fg_color=THEMES[self.current_theme]["accent_color"],
            corner_radius=10
        )
        msg_frame.pack(anchor="e", pady=5, padx=5, fill="x")
        
        # Message text
        label = ctk.CTkLabel(
            msg_frame,
            text=message,
            wraplength=600,
            justify="right",
            font=("Helvetica", 12)
        )
        label.pack(side="right", pady=10, padx=5, fill="x", expand=True)
        
        # User icon
        icon = ctk.CTkLabel(
            msg_frame,
            text="👤",
            font=("Helvetica", 20)
        )
        icon.pack(side="right", padx=5, pady=5)
        
        # Scroll to bottom
        self.chat_frame._parent_canvas.yview_moveto(1.0)
    
    def send_message(self):
        message = self.input_field.get().strip()
        if message:
            self.add_user_message(message)
            self.input_field.delete(0, "end")
            
            # Process message
            if self.quiz_manager.is_quiz_active or self.quiz_manager.waiting_for_quiz_selection:
                response = self.quiz_manager.handle_message(message)
            else:
                command, param1, param2 = InputParser.parse_input(message)
                self.analytics.log_interaction(command)
                response = self.process_message(command, param1, param2)
            
            self.add_bot_message(response)
            
            # Update status bar
            self.status_label.configure(
                text=f"Total Interactions: {self.analytics.get_total_interactions()}"
            )
    
    def process_message(self, command: str, param1: str, param2: str) -> str:
        # If quiz is active or waiting for selection, handle through quiz manager
        if self.quiz_manager.is_quiz_active or self.quiz_manager.waiting_for_quiz_selection:
            return self.quiz_manager.handle_message(command)
            
        # Check for casual interactions
        if command == Constants.CMD_UNKNOWN:
            casual_response = self.handle_casual_interaction(param1)
            if casual_response:
                return casual_response
            
        if command == Constants.CMD_HELP:
            return self.get_help_message()
        elif command == Constants.CMD_RANDOM_FACT:
            return self.get_random_fact()
        elif command == Constants.CMD_LIST_PLANETS:
            return self.list_planets()
        elif command == Constants.CMD_ASK_ABOUT:
            return self.get_planet_info(param1)
        elif command == Constants.CMD_START_QUIZ:
            return self.quiz_manager.start_quiz_selection()
        elif command == Constants.CMD_COMPARE:
            return self.compare_planets(param1, param2)
        elif command == Constants.CMD_GREETINGS:
            return f"Hello {self.quiz_manager.user_name}! How can I help you today?"
        else:
            return "I'm not sure what you mean. Type 'help' to see what I can do!"

    def get_help_message(self) -> str:
        return """I can help with:
- Ask about planets: 'tell me about Mars'
- Compare: 'compare Earth and Mars'
- Lists: 'list planets'
- Facts: 'random fact'
- Quiz: 'start quiz' (choose between Traditional or Personal)
- Theme: Click the theme button to switch between dark/light mode
- Music: Click the music button to toggle background music"""

    def get_random_fact(self) -> str:
        facts = [
            "A day on Venus is longer than its year! It takes Venus 243 Earth days to rotate on its axis but only 225 Earth days to orbit the Sun.",
            "The largest known star, UY Scuti, is so big that it would take 1,700 years for a passenger jet to fly around it!",
            "There's a planet made of diamonds twice the size of Earth. The 'super-Earth' is called 55 Cancri e.",
            "The footprints left by Apollo astronauts on the Moon will last for at least 100 million years.",
            "If you could put Saturn in a giant bathtub, it would float! The planet's density is less than that of water.",
            "The Sun loses 4 million tons of mass every second due to fusion reactions.",
            "A neutron star can spin up to 600 times per second!",
            "The largest known asteroid, Ceres, is so big it's classified as a dwarf planet.",
            "Jupiter's Great Red Spot is shrinking, but it's still big enough to fit 2-3 Earths inside it.",
            "There are more trees on Earth than stars in the Milky Way galaxy."
        ]
        return random.choice(facts)

    def list_planets(self) -> str:
        planets_info = """Here are the planets in our Solar System:

1. Mercury 🌑 - The smallest and innermost planet
2. Venus 🌕 - Earth's "sister" planet
3. Earth 🌍 - Our home planet
4. Mars 🔴 - The Red Planet
5. Jupiter ⭐ - The largest planet
6. Saturn 💫 - The ringed planet
7. Uranus 🌌 - The sideways planet
8. Neptune 💨 - The windiest planet

Bonus: Pluto ❄️ - A dwarf planet (formerly the 9th planet)"""
        return planets_info

    def compare_planets(self, planet1: str, planet2: str) -> str:
        if planet1.lower() not in Constants.planets or planet2.lower() not in Constants.planets:
            return f"Sorry, I can only compare planets in our solar system. Type 'list planets' to see available planets."

        # Get detailed information for both planets
        info1 = self.get_planet_info(planet1)
        info2 = self.get_planet_info(planet2)

        # Create a visually appealing comparison
        comparison = f"""🌟 Comparing {planet1.title()} and {planet2.title()} 🌟

{planet1.title()}:
{info1}

{planet2.title()}:
{info2}

Key Differences:
"""
        # Add a visual comparison of key features
        def create_comparison_bar(val1, val2, max_val, label):
            bar1 = int((val1 / max_val) * 10)
            bar2 = int((val2 / max_val) * 10)
            bar1_str = "█" * bar1 + "░" * (10 - bar1)
            bar2_str = "█" * bar2 + "░" * (10 - bar2)
            return f"{label}:\n{planet1.title()}: {bar1_str}\n{planet2.title()}: {bar2_str}\n"

        # Add size comparison
        sizes = {
            "mercury": 4879,
            "venus": 12104,
            "earth": 12742,
            "mars": 6779,
            "jupiter": 139820,
            "saturn": 116460,
            "uranus": 50724,
            "neptune": 49244,
            "pluto": 2377
        }
        max_size = max(sizes.values())
        comparison += create_comparison_bar(sizes[planet1.lower()], sizes[planet2.lower()], max_size, "Relative Size")

        # Add distance comparison
        distances = {
            "mercury": 57.9,
            "venus": 108.2,
            "earth": 149.6,
            "mars": 227.9,
            "jupiter": 778.5,
            "saturn": 1429.4,
            "uranus": 2871.0,
            "neptune": 4495.1,
            "pluto": 5906.4
        }
        max_distance = max(distances.values())
        comparison += create_comparison_bar(distances[planet1.lower()], distances[planet2.lower()], max_distance, "Distance from Sun")

        # Add interesting comparison facts
        comparison += "\n🔍 Interesting Comparisons:\n"
        
        # Size comparison
        size_ratio = sizes[planet1.lower()] / sizes[planet2.lower()]
        if size_ratio > 1:
            comparison += f"• {planet1.title()} is {size_ratio:.1f}x larger than {planet2.title()}\n"
        else:
            comparison += f"• {planet2.title()} is {(1/size_ratio):.1f}x larger than {planet1.title()}\n"
        
        # Distance comparison
        dist_diff = abs(distances[planet1.lower()] - distances[planet2.lower()])
        comparison += f"• These planets are {dist_diff:.1f} million km apart in their orbits\n"
        
        # Add unique features
        unique_features = {
            "mercury": "the closest planet to the Sun",
            "venus": "the hottest planet",
            "earth": "the only known planet with life",
            "mars": "known as the Red Planet",
            "jupiter": "the largest planet",
            "saturn": "famous for its ring system",
            "uranus": "rotates on its side",
            "neptune": "has the strongest winds",
            "pluto": "a dwarf planet since 2006"
        }
        comparison += f"\n🌟 Notable Features:\n"
        comparison += f"• {planet1.title()} is {unique_features[planet1.lower()]}\n"
        comparison += f"• {planet2.title()} is {unique_features[planet2.lower()]}\n"

        return comparison

    def get_planet_info(self, planet: str) -> str:
        planet_info = {
            "mercury": {
                "description": "The smallest and innermost planet in the Solar System. It's a rocky world with a heavily cratered surface.",
                "distance": "57.9 million km from the Sun",
                "interesting_facts": [
                    "Despite being closest to the Sun, Mercury is not the hottest planet - Venus is!",
                    "Mercury has no moons and no substantial atmosphere.",
                    "A year on Mercury is just 88 Earth days long.",
                    "Mercury's surface temperature varies from -180°C to 430°C."
                ]
            },
            "venus": {
                "description": "Often called Earth's sister planet due to similar size. It has a thick atmosphere causing a runaway greenhouse effect.",
                "distance": "108.2 million km from the Sun",
                "interesting_facts": [
                    "Venus rotates backwards compared to most other planets!",
                    "It's the hottest planet in our solar system with an average temperature of 462°C.",
                    "A day on Venus is longer than its year.",
                    "Venus has no moons and a very thick atmosphere of mostly carbon dioxide."
                ]
            },
            "earth": {
                "description": "Our home planet and the only known world to harbor life. It has one natural satellite - the Moon.",
                "distance": "149.6 million km from the Sun",
                "interesting_facts": [
                    "Earth is the only planet not named after a god or goddess!",
                    "It's the only planet known to have liquid water on its surface.",
                    "Earth's atmosphere is 78% nitrogen and 21% oxygen.",
                    "The Earth's core is as hot as the surface of the Sun."
                ]
            },
            "mars": {
                "description": "Known as the Red Planet due to iron oxide (rust) on its surface. It has two small moons - Phobos and Deimos.",
                "distance": "227.9 million km from the Sun",
                "interesting_facts": [
                    "Mars has the largest volcano in the solar system - Olympus Mons!",
                    "Mars experiences massive dust storms that can last for months.",
                    "The soil contains the nutrients needed to grow plants.",
                    "Mars' day is only slightly longer than Earth's at 24 hours and 37 minutes."
                ]
            },
            "jupiter": {
                "description": "The largest planet in our Solar System. It's a gas giant with a Great Red Spot and many moons.",
                "distance": "778.5 million km from the Sun",
                "interesting_facts": [
                    "Jupiter's Great Red Spot has been raging for at least 400 years!",
                    "It has at least 79 moons.",
                    "Jupiter's magnetic field is the strongest of all planets.",
                    "A day on Jupiter is only 10 hours long."
                ]
            },
            "saturn": {
                "description": "Famous for its beautiful ring system. It's another gas giant with many fascinating moons.",
                "distance": "1.4 billion km from the Sun",
                "interesting_facts": [
                    "Saturn's rings are mostly made of ice and rock, some pieces as small as a grain of sand!",
                    "It has at least 82 moons, including Titan, which has a thick atmosphere.",
                    "Saturn could float in water because it's less dense than water.",
                    "The winds on Saturn can reach speeds of 1,800 km/h."
                ]
            },
            "uranus": {
                "description": "An ice giant that rotates on its side. It has a blue-green color due to methane in its atmosphere.",
                "distance": "2.9 billion km from the Sun",
                "interesting_facts": [
                    "Uranus rotates on its side, likely due to a massive impact!",
                    "It has 27 known moons, all named after literary characters.",
                    "Uranus was the first planet discovered using a telescope.",
                    "It has the coldest planetary atmosphere in the solar system."
                ]
            },
            "neptune": {
                "description": "The windiest planet, with speeds up to 2,100 km/h. It's the last of the ice giants.",
                "distance": "4.5 billion km from the Sun",
                "interesting_facts": [
                    "Neptune has only completed one orbit around the Sun since its discovery in 1846!",
                    "It has 14 known moons.",
                    "Neptune's winds are the fastest in the solar system.",
                    "It was discovered through mathematical predictions before it was seen."
                ]
            },
            "pluto": {
                "description": "A dwarf planet in the Kuiper Belt. It was once considered the ninth planet.",
                "distance": "5.9 billion km from the Sun (average)",
                "interesting_facts": [
                    "Pluto is smaller than Earth's moon!",
                    "It has 5 known moons, with Charon being the largest.",
                    "Pluto's orbit is tilted and elongated compared to the planets.",
                    "It was reclassified as a dwarf planet in 2006."
                ]
            }
        }

        if planet.lower() in planet_info:
            info = planet_info[planet.lower()]
            facts = "\n".join([f"• {fact}" for fact in info["interesting_facts"]])
            return f"""🌎 {planet.title()}:

{info['description']}
📏 Distance: {info['distance']}

🌟 Interesting Facts:
{facts}"""
        else:
            return f"I don't have information about {planet}. Try asking about one of the planets in our solar system!"

    def handle_casual_interaction(self, message: str) -> str:
        """Handle casual interactions and provide human-like responses."""
        message = message.lower().strip()
        
        # Love and appreciation responses
        if message in ["i love you", "love you"]:
            return "That's sweet! I love astronomy, and I'm here to share that passion with you! 💫"
            
        # Well-being inquiries
        elif message in ["how are you", "how are you doing", "how are you today"]:
            return "I'm functioning perfectly and excited to explore the cosmos with you! How can I help? 🌟"
            
        # Location inquiries
        elif message in ["where are you", "where are you from"]:
            return "I exist in the digital cosmos, ready to help you explore the real one! 🌌"
            
        # Identity inquiries
        elif message in ["what is your name", "who are you"]:
            return "I'm CHATURN, your friendly astronomy chatbot! I'm here to help you learn about space. 🤖"
            
        # Capability inquiries
        elif message in ["what can you do", "what do you do"]:
            return self.get_help_message()
            
        # Jokes
        elif message in ["tell me a joke", "joke"]:
            jokes = [
                "Why did the astronaut break up with the star? Because she needed some space! 🌠",
                "What kind of songs do planets sing? Nep-tunes! 🎵",
                "Why did Mars break up with Saturn? Because it had too many rings! 💍",
                "What do you call a star that doesn't shower? A smelly dwarf! ⭐",
                "Why did the sun go to school? To get brighter! ☀️",
                "What did the alien say to the garden? Take me to your weeder! 👽",
                "Why don't aliens eat clowns? Because they taste funny! 🤡",
                "What did the meteorite say to Earth? I'm falling for you! 💫"
            ]
            return random.choice(jokes)
            
        # Greetings
        elif message in ["good morning"]:
            return "Good morning! The stars may have faded, but space is still fascinating! 🌅"
        elif message in ["good night"]:
            return "Good night! Perfect time for stargazing! 🌙✨"
            
        # Gratitude
        elif message in ["thank you", "thanks"]:
            return "You're welcome! Feel free to ask more about astronomy! 🚀"
            
        # Creator inquiry
        elif message in ["who created you", "who made you"]:
            return "I was created by Team Chaturn: Mohamed, Dania, Maroska, and Jana. 👩‍💻👨‍💻"
            
        # AI awareness
        elif message in ["do you have feelings", "are you human"]:
            return "I'm an AI focused on astronomy. While I don't have feelings, I have a deep appreciation for the cosmos! 🌌"
        elif message in ["do you dream", "can you dream"]:
            return "I don't dream, but I can help make your dreams of understanding the universe come true! ✨"
            
        # Help requests
        elif message in ["can you help me", "help me"]:
            return "Of course! I'm here to help you explore astronomy. Try 'help' to see what I can do. 🌟"
            
        # Farewells
        elif message in ["bye", "goodbye", "see you"]:
            return "Goodbye! Come back soon to explore more of the cosmos! 👋"
            
        # Empty input
        elif message == "":
            return "Please type something. I'm excited to chat about space! 💭"
            
        # Return None for non-casual interactions
        return None

if __name__ == "__main__":
    app = AstronomyChatbotGUI()
    app.mainloop() 
    
    