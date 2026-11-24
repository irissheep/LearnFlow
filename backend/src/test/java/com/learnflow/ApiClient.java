package com.learnflow;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import com.jayway.jsonpath.JsonPath;

public class ApiClient {
    private final String base;
    private final HttpClient client;
    public ApiClient(int port) {
        base = "http://127.0.0.1:" + port;
        client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }
    public record Response(int status, String body) {
        public long number(String path) { return ((Number) JsonPath.read(body, path)).longValue(); }
        public String text(String path) { return JsonPath.read(body, path); }
        public Object json(String path) { return JsonPath.read(body, path); }
    }
    public Response get(String path) throws Exception { return request("GET", path, null, false); }
    public Response post(String path, String body) throws Exception { return request("POST", path, body, true); }
    public Response put(String path, String body) throws Exception { return request("PUT", path, body, true); }
    public Response delete(String path) throws Exception { return request("DELETE", path, null, true); }
    public Response request(String method, String path, String body, boolean csrf) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(45));
        if (csrf) {
            Response token = get("/api/auth/csrf");
            request.header(token.text("$.headerName"), token.text("$.token"));
        }
        request.header("Content-Type", "application/json");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Response(response.statusCode(), response.body());
    }
    public void registerAndLogin(String username) throws Exception {
        String credentials = "{\"username\":\"" + username + "\",\"password\":\"test-password-123\",\"nickname\":\"Learner\"}";
        if (post("/api/auth/register", credentials).status() != 201) throw new AssertionError("Registration failed");
        if (post("/api/auth/login", credentials).status() != 200) throw new AssertionError("Login failed");
    }
}
