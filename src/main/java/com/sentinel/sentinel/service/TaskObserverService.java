package com.sentinel.sentinel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.sentinel.Task;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
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

        oneTask.register();
        secondTask.register();
        thirdTask.register();

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

            if (task.observe()) {
                System.out.println("✔ Task complete: " + task.getQuery());
                task.setComplete(true);
                taskList.remove(task);
            }
        };

        scheduler.scheduleAtFixedRate(check, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
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
