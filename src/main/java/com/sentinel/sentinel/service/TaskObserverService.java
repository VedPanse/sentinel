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
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class TaskObserverService {

    private final PythonProcessManager matcher = new PythonProcessManager();
    private int matcherPort;

    private final List<Task> taskList = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private static final int CHECK_INTERVAL_SECONDS = 30; // For testing. Change to MINUTES in prod
    private static final String FILE_NAME = ".task_log.json";

    @PostConstruct
    public void startObserving() {
        try {
            matcherPort = matcher.findAvailablePort(); // dynamically find free port
            matcher.startPythonServer(matcherPort);

            // ⏳ Wait until Flask server is healthy
            waitForPythonHealth(matcherPort);
            System.out.println("✅ Python matcher is healthy");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("❌ Could not start or connect to Python matcher", e);
        }

        System.out.println("🔁 Loading tasks from " + FILE_NAME);

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
                int responseCode = conn.getResponseCode();

                if (responseCode == 200) return; // ready
            } catch (IOException ignored) {
            }
            Thread.sleep(delayMs);
        }
        throw new IOException("Timed out waiting for Python matcher to become healthy on port " + port);
    }


    private void scheduleTask(Task task) {
        Runnable check = () -> {
            System.out.println("🕵 Checking task: " + task.getId());
            try {
                boolean matched = sendToPythonMatcher(task.getTarget(), task.getQuery());

                if (matched) {
                    System.out.println("✔ Task complete: " + task.getQuery());
                    task.setComplete(true);
                    taskList.remove(task);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Error checking task: " + task.getId());
                e.printStackTrace();
            }
        };

        scheduler.scheduleAtFixedRate(check, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private boolean sendToPythonMatcher(String url, String query) throws IOException {
        URL endpoint = new URL("http://localhost:" + matcherPort + "/match");
        HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        ObjectMapper mapper = new ObjectMapper();
        String jsonPayload = mapper.writeValueAsString(Map.of("url", url, "query", query));

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new IOException("Server returned non-200 status: " + status);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
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
