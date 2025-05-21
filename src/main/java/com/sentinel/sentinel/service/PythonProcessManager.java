package com.sentinel.sentinel.service;

import java.io.IOException;
import java.net.ServerSocket;

public class PythonProcessManager {
    private Process pythonProcess;

    public void startPythonServer(int port) throws IOException {
        String pythonScript = "sentinel.py";
        ProcessBuilder pb = new ProcessBuilder(
                "bash", "-c", "source .venv/bin/activate && python " + pythonScript + " --port=" + port
        );
        pb.inheritIO(); // so logs show in Java console
        pythonProcess = pb.start();

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
