package com.example.feedback;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Minimal Java 17 application using JDK built-in HttpServer.
 * Serves src/main/resources/index.html and handles POST /submit.
 */
public class App {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/", new StaticFileHandler());
        server.createContext("/submit", new SubmitHandler());

        // Use thread pool executor for handling concurrent requests
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("==================================================");
        System.out.println(" Feedback Application started successfully!");
        System.out.println(" Local URL: http://localhost:" + PORT);
        System.out.println(" Listening on port " + PORT + "...");
        System.out.println("==================================================");
    }

    /**
     * Serves index.html from classpath resources (or fallback to file system).
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (!"GET".equalsIgnoreCase(method)) {
                sendResponse(exchange, 405, "text/plain; charset=UTF-8", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
                return;
            }

            // Only serve root "/" or "/index.html"
            if (!"/".equals(path) && !"/index.html".equals(path)) {
                sendResponse(exchange, 404, "text/plain; charset=UTF-8", "404 Not Found".getBytes(StandardCharsets.UTF_8));
                return;
            }

            byte[] htmlBytes = loadIndexHtml();
            if (htmlBytes == null) {
                sendResponse(exchange, 500, "text/plain; charset=UTF-8", "index.html not found".getBytes(StandardCharsets.UTF_8));
                return;
            }

            sendResponse(exchange, 200, "text/html; charset=UTF-8", htmlBytes);
        }

        private byte[] loadIndexHtml() throws IOException {
            // Attempt 1: Load from classpath (standard for packaged JAR)
            try (InputStream in = App.class.getResourceAsStream("/index.html")) {
                if (in != null) {
                    return in.readAllBytes();
                }
            }

            // Attempt 2: Load via ClassLoader
            try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("index.html")) {
                if (in != null) {
                    return in.readAllBytes();
                }
            }

            // Attempt 3: Direct filesystem fallback (development convenience)
            Path devPath = Path.of("src", "main", "resources", "index.html");
            if (Files.isRegularFile(devPath)) {
                return Files.readAllBytes(devPath);
            }

            return null;
        }
    }

    /**
     * Handles POST /submit form submission and parses application/x-www-form-urlencoded data.
     */
    static class SubmitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "text/plain; charset=UTF-8", "Method Not Allowed. Use POST.".getBytes(StandardCharsets.UTF_8));
                    return;
                }

                // Read the request body
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, length);
                }
                String rawBody = baos.toString(StandardCharsets.UTF_8);

                // Parse form fields
                Map<String, String> formData = parseFormData(rawBody);

                String name = formData.getOrDefault("name", "").trim();
                String email = formData.getOrDefault("email", "").trim();
                String category = formData.getOrDefault("category", "").trim();
                String rating = formData.getOrDefault("rating", "").trim();
                String comments = formData.getOrDefault("comments", "").trim();

                // Log received submission to console
                System.out.println("--------------------------------------------------");
                System.out.println("Feedback Received:");
                System.out.println("Name:     " + name);
                System.out.println("Email:    " + email);
                System.out.println("Category: " + category);
                System.out.println("Rating:   " + rating + " / 5");
                System.out.println("Comments: " + comments);
                System.out.println("--------------------------------------------------");

                // Generate HTML response
                String responseHtml = generateConfirmationHtml(name, email, category, rating, comments);
                byte[] responseBytes = responseHtml.getBytes(StandardCharsets.UTF_8);

                sendResponse(exchange, 200, "text/html; charset=UTF-8", responseBytes);
            } catch (Throwable t) {
                t.printStackTrace();
                byte[] errorBytes = ("Internal Server Error: " + t.getMessage()).getBytes(StandardCharsets.UTF_8);
                sendResponse(exchange, 500, "text/plain; charset=UTF-8", errorBytes);
            }
        }

        private Map<String, String> parseFormData(String body) {
            if (body == null || body.isBlank()) {
                return Collections.emptyMap();
            }
            Map<String, String> map = new HashMap<>();
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                if (pair.isEmpty()) continue;
                int idx = pair.indexOf('=');
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String val = (idx < pair.length() - 1)
                            ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8)
                            : "";
                    map.put(key, val);
                } else {
                    String key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                    map.put(key, "");
                }
            }
            return map;
        }

        private String generateConfirmationHtml(String name, String email, String category, String rating, String comments) {
            String template = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Feedback Received</title>
                    <style>
                        :root {
                            --primary: #2563eb;
                            --primary-hover: #1d4ed8;
                            --bg: #f8fafc;
                            --card-bg: #ffffff;
                            --text: #1e293b;
                            --text-muted: #64748b;
                            --success: #16a34a;
                            --border: #e2e8f0;
                        }
                        * { box-sizing: border-box; margin: 0; padding: 0; }
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                            background-color: var(--bg);
                            color: var(--text);
                            line-height: 1.6;
                            padding: 2rem 1rem;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                        }
                        .container {
                            background: var(--card-bg);
                            border: 1px solid var(--border);
                            border-radius: 12px;
                            box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.01);
                            width: 100%;
                            max-width: 580px;
                            padding: 2.5rem;
                        }
                        .badge {
                            display: inline-flex;
                            align-items: center;
                            gap: 0.5rem;
                            background-color: #dcfce7;
                            color: var(--success);
                            padding: 0.4rem 0.9rem;
                            border-radius: 9999px;
                            font-size: 0.875rem;
                            font-weight: 600;
                            margin-bottom: 1rem;
                        }
                        h1 {
                            font-size: 1.75rem;
                            font-weight: 700;
                            color: var(--text);
                            margin-bottom: 0.5rem;
                        }
                        p.subtitle {
                            color: var(--text-muted);
                            margin-bottom: 2rem;
                        }
                        .details-card {
                            background: #f1f5f9;
                            border-radius: 8px;
                            padding: 1.25rem 1.5rem;
                            margin-bottom: 2rem;
                        }
                        .detail-row {
                            display: flex;
                            justify-content: space-between;
                            padding: 0.5rem 0;
                            border-bottom: 1px solid #e2e8f0;
                            font-size: 0.95rem;
                        }
                        .detail-row:last-child {
                            border-bottom: none;
                            flex-direction: column;
                            gap: 0.25rem;
                        }
                        .label {
                            font-weight: 600;
                            color: var(--text-muted);
                        }
                        .value {
                            font-weight: 500;
                            color: var(--text);
                            text-align: right;
                        }
                        .detail-row:last-child .value {
                            text-align: left;
                            white-space: pre-wrap;
                            background: #ffffff;
                            padding: 0.75rem;
                            border-radius: 6px;
                            margin-top: 0.25rem;
                            border: 1px solid var(--border);
                        }
                        .btn {
                            display: inline-block;
                            width: 100%;
                            text-align: center;
                            background-color: var(--primary);
                            color: #ffffff;
                            font-size: 1rem;
                            font-weight: 600;
                            padding: 0.875rem 1.5rem;
                            border-radius: 8px;
                            text-decoration: none;
                            transition: background-color 0.2s ease;
                        }
                        .btn:hover {
                            background-color: var(--primary-hover);
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="badge">&#10003; Submission Successful</div>
                        <h1>Thank You for Your Feedback!</h1>
                        <p class="subtitle">Your response has been recorded by the server.</p>

                        <div class="details-card">
                            <div class="detail-row">
                                <span class="label">Name:</span>
                                <span class="value">{{NAME}}</span>
                            </div>
                            <div class="detail-row">
                                <span class="label">Email:</span>
                                <span class="value">{{EMAIL}}</span>
                            </div>
                            <div class="detail-row">
                                <span class="label">Category:</span>
                                <span class="value">{{CATEGORY}}</span>
                            </div>
                            <div class="detail-row">
                                <span class="label">Rating:</span>
                                <span class="value">{{RATING}} / 5</span>
                            </div>
                            <div class="detail-row">
                                <span class="label">Comments:</span>
                                <div class="value">{{COMMENTS}}</div>
                            </div>
                        </div>

                        <a href="/" class="btn">Submit Another Feedback</a>
                    </div>
                </body>
                </html>
                """;

            return template
                    .replace("{{NAME}}", escapeHtml(name))
                    .replace("{{EMAIL}}", escapeHtml(email))
                    .replace("{{CATEGORY}}", escapeHtml(category))
                    .replace("{{RATING}}", escapeHtml(rating))
                    .replace("{{COMMENTS}}", escapeHtml(comments));
        }

        private String escapeHtml(String input) {
            if (input == null) return "";
            return input.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#39;");
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, byte[] responseBytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
            os.flush();
        }
    }
}
