package com.sentinel.sentinel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;


import java.io.File;
import java.util.UUID;
public class Task {
    private final String uuid;
    private final String FILE_NAME = "task_log.json";
    private String target;
    private String query;
    private boolean completed;

    Task(String target, String query, boolean completed){
        this.target = target;
        this.query = query;
        this.completed = completed;
        this.uuid = UUID.randomUUID().toString();
    }

    public boolean isComplete(){
        return completed;
    }

    public String getId(){
        return uuid;
    }

    public void setComplete(boolean completed){
        this.completed = completed;
    }

    // Getters and setters for target and query if needed
    public String getTarget() {
        return target;
    }

    public String getQuery() {
        return query;
    }

    /**
     * Tracks whether query has been satisfied at the target url
     */
    public boolean observe(){
        return false;
    }

    /**
     * Adds Task to the tasklist in the json file
     */
    public boolean register(){
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(FILE_NAME);

        try {
            ObjectNode root;

            // If file exists, load it
            if (file.exists()) {
                root = (ObjectNode) mapper.readTree(file);
            }
            else{
                // If not, create a new JSON object with an empty taskList
                root = mapper.createObjectNode();
                root.putArray("tasklist");
            }

            // Get the tasklist array reference
            ArrayNode taskList = (ArrayNode) root.withArray("taskList");

            // Add current task as JSON
            taskList.add(this.toJson());

            // Write updated JSON back to file
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);

            return true;
        } catch (Exception e) {
            e.printStackTrace();;
            return false;
        }
    }

    /**
     * Removes Task from the tasklist in the json file
     */
    public boolean removeTrace(){
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(FILE_NAME);

        try{
            // If file does not exist, nothing to remove
            if(!file.exists()){
                return false;
            }

            // Load JSON root
            ObjectNode root = (ObjectNode) mapper.readTree(file);
            ArrayNode tasklist = (ArrayNode) root.withArray("tasklist");

            // Iterate and remove matching task by UUID
            for(int i =0; i < tasklist.size();i++){
                JsonNode taskNode = tasklist.get(i);
                if (taskNode.has("uuid") && taskNode.get("uuid").asText().equals(this.getId().toString())){
                    tasklist.remove(i);
                    break;
                }
            }

            // Writing the updated JSON back to file
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
            return true;
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Returns the task in the form of a JSON object
     */
    public JsonNode toJson(){
        ObjectMapper mapper = new ObjectMapper();
        return mapper.valueToTree(this);
    }

}
