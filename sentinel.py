from flask import Flask, request, jsonify
from bs4 import BeautifulSoup
from sentence_transformers import SentenceTransformer, util
import requests
import logging

app = Flask(__name__)
model = None
model_ready = False  # <- Track readiness

# Set up logging
logging.basicConfig(level=logging.INFO)

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

        match_result = similarity > 0.3  # You can tune this threshold
        logging.info(f"{'✅ Match found' if match_result else '❌ No match'} for URL: {url}")
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
