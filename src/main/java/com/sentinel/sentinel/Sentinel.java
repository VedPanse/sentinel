package com.sentinel.sentinel;

import com.sentinel.sentinel.service.PythonProcessManager;
import com.sentinel.sentinel.service.TaskObserverService;

/**
 * Sentinel SDK entrypoint.
 *
 * Provides methods to start the matching engine and register tasks that
 * track semantic conditions on web pages using natural language.
 *
 * Usage:
 * <pre>
 *     Sentinel.start();
 *     Task task = Sentinel.register("https://example.com", "Some query");
 *     if (task.isComplete()) {
 *         System.out.println("🎉 Match found!");
 *     }
 * </pre>
 */
public class Sentinel {

    private static boolean initialized = false;
    private static TaskObserverService observer;

    /**
     * Initializes the Sentinel engine.
     * - Starts the Python NLP server (Flask app)
     * - Starts the scheduled task checking service
     */
    public static void start() {
        if (initialized) return;

        // TODO: Launch Python matcher (e.g., sentinel.py) via PythonProcessManager
        // TODO: Instantiate and start TaskObserverService

        // Example:
        // PythonProcessManager.launchMatcher();
        // observer = new TaskObserverService();
        // observer.startWatching();

        initialized = true;
    }

    /**
     * Registers a new task for observation.
     *
     * @param url The URL of the webpage to observe.
     * @param query The natural language condition to match (e.g., "Ved says he likes peanuts").
     * @return A Task object that can be queried for completion status.
     */
    public static Task register(String url, String query) {
        // TODO: Create a Task instance with a unique ID and given URL + query
        // TODO: Add it to the observer's task list
        // return the Task object

        return null; // stub
    }

    /**
     * Returns the task observer instance (for debugging or testing).
     * @return the internal TaskObserverService, if initialized
     */
    public static TaskObserverService getObserver() {
        return observer;
    }
}
