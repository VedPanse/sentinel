from flask import Flask, request, jsonify
from bs4 import BeautifulSoup
from sentence_transformers import SentenceTransformer, util
import requests
import logging
import json
import os

app = Flask(__name__)
model = None
model_ready = False

TASK_LOG_FILE = ".task_log.json"
SIMILARITY_THRESHOLD = 0.65


# Helper to mark a task complete in the JSON file
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


@app.route("/match", methods=["POST"])
def match():
    if not model_ready:
        return jsonify({"error": "Model not ready"}), 503

    logging.info("📥 Incoming request to /match")
    headers = dict(request.headers)
    logging.info(f"🔍 Headers: {headers}")

    data = request.get_json()
    url = data.get("url")
    query = data.get("query")
    task_id = data.get("id")  # New

    logging.info(f"📦 Body: {data}")

    if not url or not query:
        logging.warning("❌ Missing URL or query.")
        return jsonify({"error": "Missing URL or query"}), 400

    try:
        response = requests.get(url, timeout=8)
        soup = BeautifulSoup(response.text, "html.parser")
        page_text = soup.get_text(separator=' ', strip=True)

        query_embedding = model.encode(query, convert_to_tensor=True)
        page_embedding = model.encode(page_text, convert_to_tensor=True)

        similarity = util.pytorch_cos_sim(query_embedding, page_embedding).item()
        logging.info(f"🧠 Similarity score: {similarity:.4f}")

        match_result = similarity > SIMILARITY_THRESHOLD
        logging.info(f"{'✅ Match found' if match_result else '❌ No match'} for URL: {url}")

        if match_result and task_id:
            mark_task_complete_by_id(task_id)

        return jsonify({"matched": match_result})

    except Exception as e:
        logging.exception("🔥 Error processing request")
        return jsonify({"error": str(e)}), 500


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"ready": model_ready}), 200 if model_ready else 503


if __name__ == "__main__":
    logging.info("🚀 Loading model...")
    try:
        model = SentenceTransformer("all-MiniLM-L6-v2")
        model_ready = True
        logging.info("✅ Model ready.")
    except Exception as e:
        logging.exception("❌ Failed to load model")
        exit(1)

    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, default=5001)
    args = parser.parse_args()

    app.run(host="0.0.0.0", port=args.port)
