from flask import Flask, request, jsonify
from bs4 import BeautifulSoup
from transformers import pipeline
import requests
import logging
import json
import os
import re
import traceback
from typing import Optional

# 🧠 Load entailment model (zero-shot with RoBERTa)
def load_model():
    return pipeline("zero-shot-classification", model="roberta-large-mnli", device=-1)

nli = load_model()
app = Flask(__name__)
model_ready = True
TASK_LOG_FILE = ".task_log.json"

# 🔎 Regex sentence tokenizer
def sent_tokenize(text):
    return re.split(r'(?<=[.!?])\s+(?=[A-Z])', text.strip())

# 🔍 Detect numeric condition in query
def parse_numeric_condition(query: str) -> Optional[tuple[str, float]]:
    query = query.lower()
    if match := re.search(r"(?:above|over|more than|greater than)\s*\$?(\d+(?:,\d{3})*(?:\.\d+)?)", query):
        return (">", float(match.group(1).replace(",", "")))
    if match := re.search(r"(?:below|under|less than|fewer than)\s*\$?(\d+(?:,\d{3})*(?:\.\d+)?)", query):
        return ("<", float(match.group(1).replace(",", "")))
    return None

# ✅ Match query on page
def page_entails(url: str, query: str) -> bool:
    try:
        response = requests.get(url, timeout=8)
        soup = BeautifulSoup(response.text, "html.parser")
        page_text = soup.get_text(separator=" ", strip=True)[:5000]

        logging.info(f"🔢 Full HTML content (trimmed):\n{page_text[:1000]}")

        condition = parse_numeric_condition(query)

        for sentence in sent_tokenize(page_text):
            if condition:
                operator, threshold = condition
                if num_match := re.search(r"(\d+(?:,\d{3})*(?:\.\d+)?)", sentence):
                    value = float(num_match.group(1).replace(",", ""))
                    logging.info(f"🔢 Checking numeric in sentence: '{sentence}'")
                    if (operator == ">" and value > threshold) or (operator == "<" and value < threshold):
                        logging.info(f"📊 Numeric match: {value} {operator} {threshold}")
                        return True
            else:
                result = nli(sentence, candidate_labels=[query], hypothesis_template="This text implies that {}")
                label = result["labels"][0]
                score = result["scores"][0]
                logging.info(f"🔍 \"{sentence}\" → {label} (score: {score:.2f})")

                if label == query and score > 0.7:
                    logging.info(f"✅ Match: \"{sentence}\"")
                    return True

        logging.info("❌ No entailment or numeric match found.")
        return False

    except Exception as e:
        logging.exception("🔥 Error during entailment check")
        return False

# ✅ Mark task complete
def mark_task_complete_by_id(task_id):
    try:
        if not os.path.exists(TASK_LOG_FILE):
            logging.warning("⚠️ Task log file not found.")
            return

        with open(TASK_LOG_FILE, "r", encoding="utf-8") as f:
            data = json.load(f)

        changed = False
        for task in data.get("taskList", []):
            if task.get("id") == task_id and not task.get("complete", False):
                task["complete"] = True
                changed = True

        if changed:
            with open(TASK_LOG_FILE, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=2)
            logging.info(f"📂 Marked task {task_id} as complete in {TASK_LOG_FILE}")
    except Exception as e:
        logging.exception("❌ Failed to update task log file")

# 📡 POST /match
@app.route("/match", methods=["POST"])
def match():
    if not model_ready:
        return jsonify({"error": "Model not ready"}), 503

    logging.info("📥 Incoming request to /match")
    data = request.get_json()
    url = data.get("url")
    query = data.get("query")
    task_id = data.get("id")

    logging.info(f"🛆 Body: {data}")
    if not url or not query:
        return jsonify({"error": "Missing URL or query"}), 400

    try:
        entails = page_entails(url, query)
        if entails and task_id:
            mark_task_complete_by_id(task_id)
        return jsonify({"matched": entails})
    except Exception as e:
        logging.exception("🔥 Error processing request")
        return jsonify({"error": str(e)}), 500

# ⚕️ GET /health
@app.route("/health", methods=["GET"])
def health():
    return jsonify({"ready": model_ready}), 200 if model_ready else 503

# 🚀 Main
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, default=5001)
    args = parser.parse_args()
    app.run(host="0.0.0.0", port=args.port)
