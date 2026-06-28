package day.pithos.twitchdownloader4j.modules;

import day.pithos.twitchdownloader4j.config.TwitchConfig;
import day.pithos.twitchdownloader4j.data.Vod;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitchApiClient {

    private static final Pattern VIDEO_URL_PATTERN = Pattern.compile("/videos/(\\d+)");

    private final OkHttpClient httpClient;
    private final String clientId;
    private final String clientSecret;
    private final String accessToken;

    private TwitchApiClient(OkHttpClient httpClient, String clientId, String clientSecret, String accessToken) {
        this.httpClient = httpClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.accessToken = accessToken;
    }

    public static TwitchApiClient create(OkHttpClient httpClient, String clientId, String clientSecret) throws IOException {
        String url = TwitchConfig.OAUTH_TOKEN_URL + "?client_id=" + clientId + "&client_secret=" + clientSecret + "&grant_type=client_credentials";
        Request request = new Request.Builder().url(url).post(RequestBody.create(new byte[0])).build();
        String body = executeStatic(httpClient, request);
        String accessToken = new JSONObject(body).getString("access_token");
        return new TwitchApiClient(httpClient, clientId, clientSecret, accessToken);
    }

    private static String executeStatic(OkHttpClient httpClient, Request request) throws IOException {
        Response res = httpClient.newCall(request).execute();
        if (!res.isSuccessful()) {
            res.close();
            throw new IOException("HTTP request failed: " + res);
        }
        try (res) {
            String body = res.body().string();
            if (body == null) {
                throw new IOException("Response body is null for: " + request.url());
            }
            return body;
        }
    }

    // --- 汎用 HTTP メソッド ---

    public Response execute(Request request) throws IOException {
        Response res = httpClient.newCall(request).execute();
        if (!res.isSuccessful()) {
            res.close();
            throw new IOException("HTTP request failed: " + res);
        }
        return res;
    }

    public String executeAsString(Request request) throws IOException {
        try (Response res = execute(request)) {
            String body = res.body().string();
            if (body == null) {
                throw new IOException("Response body is null for: " + request.url());
            }
            return body;
        }
    }

    public byte[] executeAsBytes(Request request) throws IOException {
        try (Response res = execute(request)) {
            byte[] body = res.body().bytes();
            if (body == null) {
                throw new IOException("Response body is null for: " + request.url());
            }
            return body;
        }
    }

    // --- GQL リクエスト構築 ---

    public Request createCommentGqlRequest(String vodId, int startOffset, String cursor) {
        JSONObject variables = new JSONObject()
                .put("videoID", vodId);
        if (cursor == null) {
            variables.put("contentOffsetSeconds", startOffset);
        } else {
            variables.put("cursor", cursor);
        }

        JSONObject initialQuery = new JSONObject()
                .put("operationName", "VideoCommentsByOffsetOrCursor")
                .put("variables", variables)
                .put("extensions", new JSONObject()
                        .put("persistedQuery", new JSONObject()
                                .put("version", 1)
                                .put("sha256Hash", TwitchConfig.COMMENTS_QUERY_HASH)));
        JSONArray queryArray = new JSONArray().put(initialQuery);
        RequestBody requestBody = RequestBody.create(queryArray.toString(), MediaType.get(TwitchConfig.APPLICATION_JSON));

        return new Request.Builder().url(TwitchConfig.GQL_URL)
                .addHeader("Client-Id", TwitchConfig.GQL_CLIENT_ID_COMMENTS)
                .post(requestBody).build();
    }

    // --- Helix API ---

    public Vod getVodData(String vodUrl) throws IllegalArgumentException, IOException {
        Matcher matcher = VIDEO_URL_PATTERN.matcher(vodUrl);
        if (matcher.find()) {
            String vodId = matcher.group(1);
            String url = TwitchConfig.HELIX_VIDEOS_URL + "?id=" + vodId;
            Request request = new Request.Builder().url(url)
                    .addHeader("Client-Id", this.clientId)
                    .addHeader("Authorization", "Bearer " + this.accessToken).get().build();
            String body = executeAsString(request);
            return Vod.fromJsonObject(new JSONObject(body));
        } else {
            throw new IllegalArgumentException("Invalid URL!");
        }
    }

}