package com.sentinel.sentinel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.sentinel.Task;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Component
public class TaskObserverService {

    private final PythonProcessManager matcher = new PythonProcessManager();
    private int matcherPort;

    private final List<Task> taskList = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private static final int CHECK_INTERVAL_SECONDS = 30;
    private static final String FILE_NAME = ".task_log.json";

    @PostConstruct
    public void startObserving() {
        try {
            matcherPort = matcher.findAvailablePort();
            matcher.startPythonServer(matcherPort);
            waitForPythonHealth(matcherPort);
            System.out.println("✅ Python matcher is healthy");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("❌ Could not start or connect to Python matcher", e);
        }

        System.out.println("🔁 Loading tasks from " + FILE_NAME);

        File file = new File(FILE_NAME);
        if (!file.exists()) {
            register();  // Populate .task_log.json if missing
        }

        List<Task> loaded = loadTasksFromFile();
        taskList.addAll(loaded);

        for (Task task : loaded) {
            scheduleTask(task);
            System.out.println("📌 Scheduled task: " + task.getQuery());
        }
    }

    private void waitForPythonHealth(int port) throws IOException, InterruptedException {
        int retries = 20;
        int delayMs = 500;

        while (retries-- > 0) {
            try {
                URL healthURL = new URL("http://localhost:" + port + "/health");
                HttpURLConnection conn = (HttpURLConnection) healthURL.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                if (conn.getResponseCode() == 200) return;
            } catch (IOException ignored) {}
            Thread.sleep(delayMs);
        }
        throw new IOException("Timed out waiting for Python matcher to become healthy on port " + port);
    }

    private void register() {
        Task one = new Task("https://results.cbse.nic.in/", "When CBSE announces class X result");
        Task two = new Task("https://vedpanse.com", "When he posts a blog");
        Task three = new Task("https://x.com/cbseindia29", "When he posts a blog");
        Task four = new Task("https://www.apple.com/newsroom/2025/05/apples-worldwide-developers-conference-kicks-off-june-9/", "When apple announces Worldwide Developer's Conference dates, send a message to my mom.");
        Task five = new Task("https://www.apple.com/newsroom/2025/05/apples-worldwide-developers-conference-kicks-off-june-9/", "When Microsoft announces shutdown, send a message on Slack");

        taskList.addAll(List.of(one, two, three, four, five));
        taskList.forEach(Task::register);
        System.out.println("✅ Registered 3 demo tasks");
    }

    private void scheduleTask(Task task) {
        Runnable check = () -> {
            System.out.println("🕵 Checking task: " + task.getId());
            try {
                boolean matched = sendToPythonMatcher(task.getTarget(), task.getQuery(), task.getId());

                if (matched) {
                    System.out.println("✔ Task complete: " + task.getQuery());
                    task.setComplete(true); // ✅ Will persist itself
                    taskList.remove(task);  // Optional: remove from memory
                }
            } catch (Exception e) {
                System.err.println("⚠️ Error checking task: " + task.getId());
                e.printStackTrace();
            }
        };

        scheduler.scheduleAtFixedRate(check, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private boolean sendToPythonMatcher(String url, String query, String taskId) throws IOException {
        URL endpoint = new URL("http://localhost:" + matcherPort + "/match");
        HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        ObjectMapper mapper = new ObjectMapper();
        String jsonPayload = mapper.writeValueAsString(Map.of(
                "id", taskId,
                "url", url,
                "query", query
        ));

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new IOException("Server returned non-200 status: " + status);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
            return response.toString().contains("true");
        }
    }

    private List<Task> loadTasksFromFile() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            File file = new File(FILE_NAME);
            if (!file.exists()) return List.of();

            JsonNode root = mapper.readTree(file);
            JsonNode array = root.get("taskList");
            if (array == null || !array.isArray()) return List.of();

            return mapper.readerForListOf(Task.class).readValue(array);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
