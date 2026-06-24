import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Visualizer {
    public static void visualize(Tree tree) {
        if (tree == null || tree.root == null) {
            return;
        }

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            String html = buildHtml(tree);

            server.createContext("/", new HtmlHandler(html));
            server.createContext("/tree.json", new JsonHandler(tree));
            server.createContext("/save", new SaveHandler(tree));
            server.createContext("/assets/", new AssetHandler());
            server.setExecutor(null);
            server.start();

            int port = server.getAddress().getPort();
            URI uri = new URI("http://localhost:" + port + "/");
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            } else {
                System.out.println("Open your browser at " + uri);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Cannot start browser visualizer", e);
        }
    }

    private static String buildHtml(Tree tree) {
        try {
            Path htmlPath = Path.of("visualizer.html");
            if (!Files.exists(htmlPath)) {
                throw new IOException("Cannot find visualizer.html in project root: " + htmlPath.toAbsolutePath());
            }
            return Files.readString(htmlPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load visualizer HTML", e);
        }
    }

    private static String buildJson(Node root) {
        StringBuilder out = new StringBuilder();
        serializeNode(out, root);
        return out.toString();
    }

    private static void serializeNode(StringBuilder out, Node node) {
        if (node == null) {
            out.append("null");
            return;
        }
        out.append('{');
        appendJsonProperty(out, "label", node.getSkill());
        appendJsonProperty(out, "color", colorToHex(node.getSkillLevel()));
        node.getURL().ifPresentOrElse(
            url -> appendJsonProperty(out, "url", url),
            () -> out.append("\"url\":null,")
        );
        out.append("\"children\":");
        out.append('[');
        List<Node> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            serializeNode(out, children.get(i));
            if (i + 1 < children.size()) out.append(',');
        }
        out.append(']');
        out.append('}');
    }

    private static String colorToHex(Tree.SkillLevel level) {
        java.awt.Color color = level.getColor();
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static void appendJsonProperty(StringBuilder out, String name, String value) {
        out.append('"').append(name).append('"').append(':');
        out.append('"').append(escapeJson(value)).append('"').append(',');
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        StringBuilder escaped = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '\\': escaped.append("\\\\"); break;
                case '"': escaped.append("\\\""); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default:
                    if (c < 0x20 || c > 0x7E) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return escaped.toString();
    }

    private static class HtmlHandler implements HttpHandler {
        private final byte[] htmlBytes;

        HtmlHandler(String html) {
            this.htmlBytes = html.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, htmlBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(htmlBytes);
            }
        }
    }

    private static class JsonHandler implements HttpHandler {
        private final Tree tree;

        JsonHandler(Tree tree) {
            this.tree = tree;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = buildJson(tree.root);
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private static class SaveHandler implements HttpHandler {
        private final Tree tree;

        SaveHandler(Tree tree) {
            this.tree = tree;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseFormData(requestBody);
            String action = params.get("action");
            int[] path = parsePath(params.get("path"));
            String name = params.get("name");

            boolean changed = false;
            Node target = tree.getNodeByPath(path);

            if (target == null && path.length > 0) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            switch (action) {
                case "cycle" -> {
                    if (target != null) {
                        target.setSkillLevel(target.getSkillLevel().next());
                        changed = true;
                    }
                }
                case "cyclePrev" -> {
                    if (target != null) {
                        target.setSkillLevel(target.getSkillLevel().prev());
                        changed = true;
                    }
                }
                case "delete" -> {
                    if (target != null && target.getParent() != null) {
                        target.getParent().removeChild(target);
                        changed = true;
                    }
                }
                case "rename" -> {
                    if (target != null && name != null && !name.isBlank()) {
                        target.setSkill(name.trim());
                        changed = true;
                    }
                }
                case "addChild" -> {
                    if (target != null && name != null && !name.isBlank()) {
                        target.addChild(new Node(name.trim(), Tree.SkillLevel.COAL, null));
                        changed = true;
                    }
                }
                case "moveLeft", "moveRight" -> {
                    if (target != null && moveSibling(target, "moveRight".equals(action) ? 1 : -1)) {
                        changed = true;
                    }
                }
                default -> {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }
            }

            if (changed) {
                tree.saveTree();
            }

            String response = buildJson(tree.root);
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }

        private static Map<String, String> parseFormData(String body) {
            Map<String, String> result = new HashMap<>();
            for (String pair : body.split("&")) {
                if (pair.isBlank()) continue;
                String[] parts = pair.split("=", 2);
                String key = urlDecode(parts[0]);
                String value = parts.length > 1 ? urlDecode(parts[1]) : "";
                result.put(key, value);
            }
            return result;
        }

        private static String urlDecode(String value) {
            try {
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return value;
            }
        }

        private static int[] parsePath(String pathParam) {
            if (pathParam == null || pathParam.isBlank()) {
                return new int[0];
            }
            String[] parts = pathParam.split(",");
            int[] path = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                try {
                    path[i] = Integer.parseInt(parts[i]);
                } catch (NumberFormatException e) {
                    path[i] = -1;
                }
            }
            return path;
        }

        private static boolean moveSibling(Node node, int direction) {
            Node parent = node.getParent();
            if (parent == null) {
                return false;
            }
            int index = parent.getChildren().indexOf(node);
            if (index == -1) {
                return false;
            }
            int targetIndex = index + direction;
            if (targetIndex < 0 || targetIndex >= parent.getChildren().size()) {
                return false;
            }
            parent.getChildren().remove(index);
            parent.getChildren().add(targetIndex, node);
            return true;
        }
    }

    private static class AssetHandler implements HttpHandler {
        private final Path assetsRoot = Path.of("assets");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (!path.startsWith("/assets/")) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            Path relativePath = Path.of(path.substring("/assets/".length()));
            if (relativePath.normalize().startsWith("..")) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }

            Path filePath = assetsRoot.resolve(relativePath).normalize();
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            String contentType = switch (getFileExtension(filePath.getFileName().toString())) {
                case "png" -> "image/png";
                case "jpg", "jpeg" -> "image/jpeg";
                case "svg" -> "image/svg+xml";
                default -> "application/octet-stream";
            };

            byte[] fileBytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }

        private static String getFileExtension(String name) {
            int index = name.lastIndexOf('.');
            return (index >= 0) ? name.substring(index + 1).toLowerCase() : "";
        }
    }
}
