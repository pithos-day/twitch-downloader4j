package day.pithos.twitchdownloader4j.modules;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

public class TwitchApiClient {

    private final OkHttpClient httpClient;
    private final String clientId;
    private final String clientSecret;
    private final String accessToken;

    public TwitchApiClient(OkHttpClient httpClient, String clientId, String clientSecret) {
        this.httpClient = httpClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.accessToken = this.getAccessToken();
    }

    private String getAccessToken() {
        String url = "https://id.twitch.tv/oauth2/token?client_id=" + this.clientId + "&client_secret=" + this.clientSecret + "&grant_type=client_credentials";
        Request request = new Request.Builder().url(url).post(RequestBody.create(new byte[0])).build();
        try (Response response = this.httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected code " + response);
            }
            JSONObject responseJson = new JSONObject(response.body().string());
            return responseJson.getString("access_token");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}