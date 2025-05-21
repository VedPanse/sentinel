# Sentinel

![[logo.png]]

**Sentinel** is an intelligent task observer that continuously monitors web pages for meaningful content changes using AI-powered semantic matching. It automatically marks tasks as complete when the content you're looking for appears — even if phrased differently.

Built with:
- 💻 Java + Spring Boot (schedules & manages tasks)
- 🧠 Python + Flask (runs a semantic similarity engine with Sentence Transformers)
- 🧾 Local JSON file for persistent task tracking

---

## 🚀 Features

- **AI-powered query matching** — Uses semantic similarity (not just keywords)
- **Auto-task tracking** — Monitors URLs periodically and flags when queries match
- **Persistent JSON state** — All tasks and statuses are stored in `.task_log.json`
- **Cross-language architecture** — Java for task management, Python for NLP
- **Pluggable and extensible** — Easily add APIs, webhooks, or dashboards

---

## 📂 Directory Structure

```

sentinel/
├── src/
│   └── main/java/com/sentinel/sentinel/
│       ├── Task.java                # Task model with JSON persistence
│       └── service/TaskObserverService.java  # Core scheduler logic
├── sentinel.py                     # Python server for semantic matching
├── .task\_log.json                  # Auto-generated task state file
└── README.md

````

---

## ⚙️ How It Works

1. **Register tasks** using predefined URLs + semantic queries.
2. **Every 30 seconds**, the Java service checks each target by sending its content and query to the Python matcher.
3. The Python server uses a Sentence Transformer to compute similarity.
4. If the similarity passes the threshold (default `0.3`), the task is marked as complete in `.task_log.json`.

---

## 🧪 Example Task

```json
{
  "id": "abc123",
  "target": "https://results.cbse.nic.in/",
  "query": "When CBSE announces class X result",
  "complete": false
}
````

Once a result page shows relevant content (even loosely worded), `complete` becomes `true`.

---

## 🛠️ Running It Locally

### Prerequisites

* Java 17+
* Python 3.9+
* Pip packages: `flask`, `requests`, `beautifulsoup4`, `sentence-transformers`

### 1. Start the Java Service

```bash
./gradlew bootRun
```

This launches the Spring Boot app and auto-starts the Python matcher.

### 2. Python Matcher (auto-launched by Java)

Located in `sentinel.py`, it loads the model `all-MiniLM-L6-v2` on startup and serves `/match`.

---

## 📡 API (internal use)

**POST** `/match`
Send page and query for semantic comparison.

```json
{
  "id": "abc123",
  "url": "https://example.com",
  "query": "When the event is announced"
}
```

Returns:

```json
{ "matched": true }
```
---

## 📜 License

MIT License. Feel free to use, modify, and contribute.

---

## 👨‍💻 Maintainer

Made by [@VedPanse](https://github.com/VedPanse) and [@devashish1914](https://github.com/devashish1914) with 💻, ☕, and a lot of `println`.
