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
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class Task {
    private final String id = UUID.randomUUID().toString();

    @JsonIgnore
    private final String FILE_NAME = ".task_log.json";

    @JsonIgnore
    private final String TASK_LIST = "taskList";

    private String target;
    private String query;
    private boolean complete = false;

    public Task() {}

    public Task(String target, String query) {
        this.target = target;
        this.query = query;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;

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
                    ((ObjectNode) taskNode).put("complete", complete);
                    break;
                }
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean observe() {
        String html = getHTMLContent();
        if (html.isEmpty()) return false;

        Set<String> queryWords = preprocess(query);
        Set<String> pageWords = preprocess(html);

        long matched = queryWords.stream().filter(pageWords::contains).count();
        double ratio = (double) matched / queryWords.size();

        System.out.printf("🔍 [%s] Matched %d of %d (%.2f)\n", query, matched, queryWords.size(), ratio);

        boolean verdict = matched >= 3 || ratio >= 0.6;
        setComplete(verdict);
        return verdict;
    }

    private Set<String> preprocess(String text) {
        Set<String> stopwords = Set.of("the", "a", "an", "of", "on", "in", "to", "by", "for", "and", "is", "are", "was", "with", "this", "that", "when");

        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .map(word -> word.replaceAll("(ing|ed|es|s)$", ""))
                .filter(w -> w.length() > 2 && !stopwords.contains(w))
                .collect(Collectors.toSet());
    }

    private String getHTMLContent() {
        try {
            Document doc = Jsoup.connect(this.target).get();
            return doc.text();
        } catch (IOException e) {
            System.err.println("Error fetching URL: " + target);
            e.printStackTrace();
            return "";
        }
    }

    public void register() {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(FILE_NAME);

        try {
            ObjectNode root;

            if (file.exists()) {
                root = (ObjectNode) mapper.readTree(file);
            } else {
                root = mapper.createObjectNode();
                root.putArray(TASK_LIST);
            }

            ArrayNode taskList = (ArrayNode) root.withArray(TASK_LIST);

            ObjectNode taskJson = mapper.createObjectNode();
            taskJson.put("id", id);
            taskJson.put("target", target);
            taskJson.put("query", query);
            taskJson.put("complete", complete);

            taskList.add(taskJson);
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeTrace() {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(FILE_NAME);

        try {
            if (!file.exists()) return;

            ObjectNode root = (ObjectNode) mapper.readTree(file);
            ArrayNode taskList = (ArrayNode) root.withArray(TASK_LIST);

            for (int i = 0; i < taskList.size(); i++) {
                JsonNode taskNode = taskList.get(i);
                if (taskNode.has("id") && taskNode.get("id").asText().equals(this.getId())) {
                    taskList.remove(i);
                    break;
                }
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JsonNode toJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("id", id);
        node.put("target", target);
        node.put("query", query);
        node.put("complete", complete);
        return node;
    }
}
