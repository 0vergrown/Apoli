package dev.overgrown.apoli.client.speech;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.overgrown.apoli.Apoli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

public final class SpeechServer {
    private HttpServer server;
    private int port = -1;
    private BiConsumer<String, String> onTranscript;
    private Runnable onNoSupport;

    public boolean start(BiConsumer<String, String> callback, Runnable noSupportCallback) {
        if (server != null) {
            return true;
        }
        this.onTranscript = callback;
        this.onNoSupport = noSupportCallback;
        try {
            this.port = findFreePort();
            this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.createContext("/", exchange -> serveResource(exchange, "index.html", "text/html; charset=utf-8"));
            server.createContext("/speech.js", exchange -> serveResource(exchange, "speech.js", "text/javascript; charset=utf-8"));
            server.createContext("/transcript", this::handleTranscript);
            server.createContext("/nosupport", this::handleNoSupport);
            server.setExecutor(null);
            server.start();
            return true;
        } catch (IOException e) {
            Apoli.LOGGER.error("[Apoli] Failed to start speech server: {}", e.getMessage());
            this.server = null;
            return false;
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            port = -1;
        }
    }

    public int port() {
        return port;
    }

    private void serveResource(HttpExchange exchange, String name, String contentType) throws IOException {
        byte[] body;
        try (InputStream stream = SpeechServer.class.getResourceAsStream("/assets/apoli/speech/" + name)) {
            if (stream == null) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            body = stream.readAllBytes();
        }
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private void handleTranscript(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        String body;
        try (InputStream stream = exchange.getRequestBody()) {
            body = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        BiConsumer<String, String> callback = this.onTranscript;
        if (callback != null) {
            String text = jsonValue(body, "text");
            String language = jsonValue(body, "language");
            if (text != null && !text.isBlank()) {
                Apoli.LOGGER.info("[Apoli] Speech transcript: \"{}\" ({})", text, language);
                callback.accept(text, language == null ? "" : language);
            }
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private void handleNoSupport(HttpExchange exchange) throws IOException {
        Runnable callback = this.onNoSupport;
        if (callback != null) {
            callback.run();
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    private static String jsonValue(String json, String key) {
        try {
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
