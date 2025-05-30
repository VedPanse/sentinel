package com.sentinel.sentinel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.*;

/**
 * Represents a monitorable task that is persistently stored in a local JSON file.
 * Each task is uniquely identified by a UUID and contains a target URL, a natural-language
 * query to track, and a boolean flag indicating completion.
 *
 * <p>The {@code Task} class supports registering, updating, removing, and serializing
 * task data to and from a local `.task_log.json` file using Jackson.
 */
@Getter
@Setter
public class Task {
    /** Unique identifier for the task. */
    private final String id = UUID.randomUUID().toString();

    @JsonIgnore
    private final String FILE_NAME = ".task_log.json";

    @JsonIgnore
    private final String TASK_LIST = "taskList";

    public String getId() {
        return id;
    }

    public boolean isComplete() {
        return complete;
    }

    public String getQuery() {
        return query;
    }

    public String getTarget() {
        return target;
    }

    /** The target URL or system this task is associated with. */
    private String target;

    /** A user-defined query or description of the condition being observed. */
    private String query;

    /** Whether this task has been marked as complete. */
    private boolean complete = false;

    /** Default constructor for deserialization frameworks. */
    public Task() {}

    /**
     * Constructs a new {@code Task} with a specified target and query.
     *
     * @param target the URL or system to be monitored
     * @param query the natural-language description of what to observe
     */
    public Task(String target, String query) {
        this.target = target;
        this.query = query;
    }

    /**
     * Sets the completion status of the task and updates its record in the task log file.
     *
     * @param complete whether the task is complete
     */
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
                    ((ObjectNode) taskNode).put("complete", complete);
                    break;
                }
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns whether the task has equal target, query, and complete variables
     * @param o
     * @return whether the task has equal target, query, and complete variables
     */
    @Override
    public boolean equals(Object o) {
        // TODO write actual implementation here
        if (this == o)
            return true;
        if (o == null || this.getClass() != o.getClass())
            return false;
        Task other = (Task)o;
        // default behaviour: check ID too
        return this.equals(other, false);

    }

    /**
     * Compares this task with another.
     * @param other the task to compare with
     * @param force if true, ignores the id during comparison
     * @return true if tasks are equal based on comparison criteria
     */
    public boolean equals(Task other, boolean force) {
        if (other == null)
            return false;

        boolean isEqual =
                Objects.equals(this.getTarget(), other.getTarget()) &&
                Objects.equals(this.getQuery(), other.getQuery()) &&
                Objects.equals(this.isComplete(), other.isComplete());

        // If force is true, there is no need to check id
        if(force)
            return isEqual;
        else
            return isEqual && this.getId().equals(other.getId());
    }

    /**
     * Registers this task by appending its JSON representation to the task log file.
     * If the file does not exist, it is created.
     */
    public void register() {
        register(false);
    }

    public void register(boolean force) {
        // TODO add an optional parameter call force, which will be set to
        //  false by default. If force is false, enable
        //  smart duplicate tracking, which will not add a task if it's
        //  .equals() method is true

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
            boolean shouldAdd;
            if(!force) {
                shouldAdd = true;
                for(JsonNode taskNode: taskList) {
                    Task existing = new Task(
                            taskNode.get("target").asText(),
                            taskNode.get("query").asText()
                    );
                    existing.setComplete(taskNode.get("complete").asBoolean());

                    // Check for exact match ignoring id
                    if (this.equals(existing, true)) {
                        shouldAdd = false;
                        break;
                    }
                }
            }
            else {
                // If force is true always add
                shouldAdd = true;
            }

            if(shouldAdd){
                ObjectNode taskJson = mapper.createObjectNode();
                taskJson.put("id", id);
                taskJson.put("target", target);
                taskJson.put("query", query);
                taskJson.put("complete", complete);

                taskList.add(taskJson);
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Removes this task's record from the task log file, effectively deleting its trace.
     */
    public void removeTrace(){
        removeTrace(true);
    }

    public void removeTrace(boolean force) {
        // TODO add an optional variable force which is true by default,
        //  and performs the same actions as before
        // If force is true, search by everything except id (look for .equals() method)
        // If force is false, continue indexing by id
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(FILE_NAME);

        try {
            if (!file.exists()) return;

            ObjectNode root = (ObjectNode) mapper.readTree(file);
            ArrayNode taskList = (ArrayNode) root.withArray(TASK_LIST);

            boolean taskRemoved = false;

            for (int i = 0; i < taskList.size(); i++) {
                // Retrieving each task from tasklist
                JsonNode taskNode = taskList.get(i);
                Task other = new Task(
                        taskNode.get("target").asText(),
                        taskNode.get("query").asText()
                );
                other.setComplete(taskNode.get("complete").asBoolean());

                if(force) {
                    // Force = true: match by content (ignore ID)
                    if(this.equals(other, true)) {
                        taskList.remove(i);
                        taskRemoved = true;
                        break;
                    }
                }
                else{
                    // Force = false: match by every parameter including ID
                    if (this.equals(other, false)) {
                        taskList.remove(i);
                        taskRemoved = true;
                        break;
                    }
                }
            }

            if (taskRemoved){
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts this task to a {@link JsonNode} representation for external use or API transmission.
     *
     * @return a {@code JsonNode} representing this task
     */
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
