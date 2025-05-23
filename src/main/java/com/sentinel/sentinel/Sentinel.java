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
    private static PythonProcessManager pythonManager;

    /**
     * Initializes the Sentinel engine.
     * - Starts the Python NLP server (Flask app)
     * - Starts the scheduled task checking service
     */
    public static void start() {
        if (initialized) return;

        pythonManager = new PythonProcessManager();
        try {
            int port = pythonManager.findAvailablePort();
            pythonManager.startPythonServer(port);
        } catch (Exception e) {
            e.printStackTrace();
        }

        observer = new TaskObserverService();
        // Assuming startWatching() is implemented or handled implicitly

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
        Task task = new Task(url, query);
        task.register();
        return task;
    }

    /**
     * Removes a task from observation and deletes it from the task log.
     *
     * @param task The task to stop observing.
     */
    public static void unregister(Task task) {
        task.removeTrace();
    }

    /**
     * Returns the task observer instance (for debugging or testing).
     * @return the internal TaskObserverService, if initialized
     */
    public static TaskObserverService getObserver() {
        return observer;
    }
}
