package com.sentinel.sentinel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.util.UUID;


@Getter
@Setter
public class Task {
    private final String id = UUID.randomUUID().toString();

    @JsonIgnore
    private final String FILE_NAME = ".task_log.json";

    @JsonIgnore
    private final String TASK_LIST = "taskList";

    // Getters and setters for target and query if needed
    private String target;
    private String query;
    private boolean completed;

    public Task() {

    }

    public Task(String target, String query, boolean completed) {
        this.target = target;
        this.query = query;
        this.completed = completed;
    }

    public boolean isComplete() {
        return completed;
    }

    public void setComplete(boolean completed) {
        this.completed = completed;

        ObjectMapper mapper = new ObjectMapper();
        File file = new File(FILE_NAME);

        try {
            if (!file.exists()) return;

            ObjectNode root = (ObjectNode) mapper.readTree(file);
            ArrayNode taskList = (ArrayNode) root.withArray(TASK_LIST);

            for (int i = 0; i < taskList.size(); i++) {
                JsonNode taskNode = taskList.get(i);
                if (taskNode.has("id") && taskNode.get("id").asText().equals(this.id)) {
                    // Update "complete" field
                    ((ObjectNode) taskNode).put("complete", completed);
                    break;
                }
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Tracks whether query has been satisfied at the target url
     */
    public boolean observe() {
        String htmlContent = getHTMLContent();
        System.out.println(htmlContent);
        boolean verdict = true;

        setCompleted(verdict);
        return verdict;
    }

    /**
     * Return the HTML content of an url
     * @return HTML content
     */
    private String getHTMLContent() {
        try {
            Document doc = Jsoup.connect(this.target).get();
            return doc.text(); // only visible text, not HTML tags
        } catch (IOException e) {
            System.err.println("Error fetching URL: " + target);
            e.printStackTrace();
            return "";
        }
    }


    /**
     * Adds Task to the tasklist in the json file
     */
    public boolean register() {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(FILE_NAME);

        try {
            ObjectNode root;

            // Load or create root object
            if (file.exists()) {
                root = (ObjectNode) mapper.readTree(file);
            } else {
                root = mapper.createObjectNode();
                root.putArray("taskList");
            }

            // Get or create taskList array
            ArrayNode taskList = (ArrayNode) root.withArray(TASK_LIST);

            // Construct task JSON manually with only the allowed fields
            ObjectNode taskJson = mapper.createObjectNode();
            taskJson.put("id", id);
            taskJson.put("target", target);
            taskJson.put("query", query);
            taskJson.put("complete", completed);

            // Append to list and write back
            taskList.add(taskJson);
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Removes Task from the tasklist in the json file
     */
    public void removeTrace() {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(FILE_NAME);

        try {
            if (!file.exists()) return;

            ObjectNode root = (ObjectNode) mapper.readTree(file);
            ArrayNode tasklist = (ArrayNode) root.withArray(TASK_LIST);

            for (int i = 0; i < tasklist.size(); i++) {
                JsonNode taskNode = tasklist.get(i);
                if (taskNode.has("id") && taskNode.get("id").asText().equals(this.getId())) {
                    // Replace old version with updated one
                    tasklist.set(i, this.toJson());
                    break;
                }
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Returns the task in the form of a JSON object
     */
    public JsonNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.valueToTree(this);
    }

}
