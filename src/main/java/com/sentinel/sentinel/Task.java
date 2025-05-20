package com.sentinel.sentinel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
public class Task {
    String target;
    String query;
    boolean completed;
    private final String uuid;

    Task(String target, String query, boolean completed){
        this.target = target;
        this.query = query;
        this.completed = completed;
        this.uuid = UUID.randomUUID().toString();
    }

    /**
     * Tracks whether query has been satisfied at the target url
     */
    public boolean observe(){
        return false;
    }

    /**
     * Returns the task in the form of a JSON file
     */
    public JsonNode toJson(){
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonobj = mapper.valueToTree(this);
        return jsonobj;
    }

    public void setComplete(boolean completed){
        this.completed = completed;
    }

    public boolean getComplete(){
        return completed;
    }

    public String getId(){
        return uuid;
    }
}
