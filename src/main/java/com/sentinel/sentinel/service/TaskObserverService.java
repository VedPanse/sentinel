package com.sentinel.sentinel.service;

import com.sentinel.sentinel.Task;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class TaskObserverService {

    private final List<Task> taskList = new ArrayList<>();
    private static final int SAMPLE_RATE = 65536;

    @PostConstruct
    public void startObserving() {
        Thread observerThread = new Thread(() -> {
            while (true) {
                System.out.println("Observer thread running");
                System.out.println("taskList: " + taskList.toString());

                if (taskList.isEmpty()) break;

                Iterator<Task> iterator = taskList.iterator();
                while (iterator.hasNext()) {
                    Task task = iterator.next();
                    if (task.observe()) {
                        task.setComplete(true);
                        iterator.remove();
                        task.removeTrace();
                    }
                }

                try {
                    Thread.sleep(1000 / SAMPLE_RATE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            System.out.println("Observer thread stopped");
        });

        observerThread.setDaemon(true);
        observerThread.start();
    }
}
