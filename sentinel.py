from flask import Flask, request, jsonify
from bs4 import BeautifulSoup
from transformers import pipeline
import requests
import logging
import json
import os

app = Flask(__name__)
nli = None
model_ready = False

TASK_LOG_FILE = ".task_log.json"

def load_model():
    global nli, model_ready
    logging.info("🚀 Loading entailment model...")
    try:
        nli = pipeline("text-classification", model="facebook/bart-large-mnli")
        model_ready = True
        logging.info("✅ Model ready.")
    except Exception as e:
        logging.exception("❌ Failed to load model")
        model_ready = False

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
            logging.info(f"💾 Marked task {task_id} as complete in {TASK_LOG_FILE}")
    except Exception as e:
        logging.exception("❌ Failed to update task log file")

def is_entailment(premise, hypothesis):
    try:
        result = nli(f"{premise} </s></s> {hypothesis}", truncation=True)[0]
        logging.info(f"📊 Entailment Result: {result}")
        return result["label"] == "ENTAILMENT"
    except Exception as e:
        logging.exception("❌ Error during entailment check")
        return False

@app.route("/match", methods=["POST"])
def match():
    if not model_ready:
        return jsonify({"error": "Model not ready"}), 503

    logging.info("📥 Incoming request to /match")
    data = request.get_json()
    url = data.get("url")
    query = data.get("query")
    task_id = data.get("id")

    logging.info(f"📦 Body: {data}")

    if not url or not query:
        logging.warning("❌ Missing URL or query.")
        return jsonify({"error": "Missing URL or query"}), 400

    try:
        response = requests.get(url, timeout=8)
        soup = BeautifulSoup(response.text, "html.parser")
        page_text = soup.get_text(separator=' ', strip=True)

        entails = is_entailment(page_text, query)
        logging.info(f"{'✅ Entailment confirmed' if entails else '❌ No entailment'} for query: {query}")

        if entails and task_id:
            mark_task_complete_by_id(task_id)

        return jsonify({"matched": entails})
    except Exception as e:
        logging.exception("🔥 Error processing request")
        return jsonify({"error": str(e)}), 500

@app.route("/health", methods=["GET"])
def health():
    return jsonify({"ready": model_ready}), 200 if model_ready else 503

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, default=5001)
    args = parser.parse_args()

    load_model()
    app.run(host="0.0.0.0", port=args.port)
