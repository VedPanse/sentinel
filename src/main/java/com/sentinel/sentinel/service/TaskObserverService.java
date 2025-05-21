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

    private final List<Task> taskList = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private static final int CHECK_INTERVAL_SECONDS = 30; // For testing. Change to MINUTES in prod
    private static final String FILE_NAME = ".task_log.json";

    @PostConstruct
    public void startObserving() {
        System.out.println("🔁 Loading tasks from " + FILE_NAME);

        Task oneTask = new Task("https://results.cbse.nic.in/", "When CBSE announces class X result");
        Task secondTask = new Task("https://vedpanse.com", "When he posts a blog");
        Task thirdTask = new Task("https://x.com/cbseindia29", "When he posts a blog");

//        oneTask.register();
//        secondTask.register();
//        thirdTask.register();

        List<Task> loaded = loadTasksFromFile();
        taskList.addAll(loaded);

        for (Task task : loaded) {
            scheduleTask(task);
            System.out.println("📌 Scheduled task: " + task.getQuery());
        }
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
        URL endpoint = new URL("http://localhost:5001/match");
        HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Use ObjectMapper to safely encode the JSON payload
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


    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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
