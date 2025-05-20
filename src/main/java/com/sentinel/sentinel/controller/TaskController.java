package com.sentinel.sentinel.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
public class TaskController {

    private final String FILE_NAME = ".task_log.json";

    @GetMapping("/api/tasks")
    public JsonNode getTasks() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            File file = new File(FILE_NAME);
            return mapper.readTree(file).get("taskList");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
