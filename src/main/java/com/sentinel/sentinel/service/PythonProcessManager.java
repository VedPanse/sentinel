package com.sentinel.sentinel.service;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;

public class PythonProcessManager {
    private Process pythonProcess;

    public void startPythonServer(int port) throws IOException {
        String pythonScript = "sentinel.py";

        // TODO make the following line cross-compatible with every operating system
        // Determining os
        String os = System.getProperty("os.name").toLowerCase();
        // Choose appropriate python executable path from virtual environment
        String pythonExecutable;
        if (os.contains("win")) {
            pythonExecutable = ".venv\\Scripts\\python.exe";
        }
        else{
            pythonExecutable = ".venv/bin/python";
        }

        // Construct command
        List<String> command = Arrays.asList(pythonExecutable, pythonScript, "--port=" + port);

        // Activates and launches python script
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO(); // so logs show in Java console
        pb.directory(new File(".")); // Ensure relative paths resolve correctly
        pythonProcess = pb.start();

        // Gracefully close the python process before the java application shuts down
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (pythonProcess != null && pythonProcess.isAlive()) {
                System.out.println("🛑 Stopping Python matcher");
                pythonProcess.destroy();
            }
        }));

        System.out.println("🐍 Started Python matcher: " + pythonScript);
    }

    public int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
