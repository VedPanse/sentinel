package com.sentinel.sentinel.service;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class PythonProcessManager {
    private Process pythonProcess;
    private String pythonPath = ".venv/bin/python"; // fallback

    public void setPythonPath(String path) {
        this.pythonPath = path;
    }

    public void startPythonServer(int port) throws IOException {
        // Resolve base directory from user.dir (project root when running from IDE)
        File projectRoot = new File(System.getProperty("user.dir"));

        String os = System.getProperty("os.name").toLowerCase();
        String pythonExecutable;

        if (os.contains("win")) {
            pythonExecutable = new File(projectRoot, ".venv\\Scripts\\python.exe").getAbsolutePath();
        } else {
            pythonExecutable = new File(projectRoot, ".venv/bin/python").getAbsolutePath();
        }

        // Extract sentinel.py from resources to a temp file
        File tempScript = extractPythonScript("python/sentinel.py");
        if (tempScript == null || !tempScript.exists()) {
            throw new FileNotFoundException("❌ Could not extract sentinel.py from resources.");
        }

        List<String> command = Arrays.asList(pythonExecutable, tempScript.getAbsolutePath(), "--port=" + port);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO(); // Show Python output in Java console
        pb.directory(projectRoot); // run from project root
        pythonProcess = pb.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (pythonProcess != null && pythonProcess.isAlive()) {
                System.out.println("🛑 Stopping Python matcher");
                pythonProcess.destroy();
            }
        }));

        System.out.println("🐍 Started Python matcher on port " + port);
    }

    /**
     * Extracts the given resource to a temp file on disk.
     *
     * @param resourcePath e.g. "python/sentinel.py"
     * @return File object pointing to the extracted temp file
     * @throws IOException if resource not found or temp creation fails
     */
    private File extractPythonScript(String resourcePath) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) return null;

        File temp = Files.createTempFile("sentinel", ".py").toFile();
        temp.deleteOnExit();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
             BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        }

        return temp;
    }

    public int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
