from flask import Flask, request, jsonify
from bs4 import BeautifulSoup
from sentence_transformers import SentenceTransformer, util
import requests
import logging

app = Flask(__name__)
model = SentenceTransformer("all-MiniLM-L6-v2")  # Small, fast, effective

# Set up logging
logging.basicConfig(level=logging.INFO)

@app.route("/match", methods=["POST"])
def match():
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

if __name__ == "__main__":
    logging.info("🚀 Starting Sentinel Matcher Service on http://localhost:5000")
    app.run(host="0.0.0.0", port=5001)
