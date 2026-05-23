package day.pithos.twitchdownloader4j;

import day.pithos.twitchdownloader4j.data.Vod;
import day.pithos.twitchdownloader4j.modules.TwitchApiClient;
import junit.framework.TestCase;
import okhttp3.OkHttpClient;

import java.io.IOException;

public class AppTest extends TestCase {

    private TwitchApiClient twitchApiClient;
    private final String clientId = System.getenv("TWITCH_CLIENT_ID");
    private final String clientSecret = System.getenv("TWITCH_CLIENT_SECRET");

    private final String vodUrl = System.getenv("TWITCH_VOD_URL");

    public void testGetVodData() {
        twitchApiClient = getTwitchApiClient();
        try {
            Vod vod = twitchApiClient.getVodData(vodUrl);
            System.out.println(vod.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TwitchApiClient getTwitchApiClient() {
        if (twitchApiClient == null) {
            try {
                twitchApiClient = new TwitchApiClient(new OkHttpClient(), clientId, clientSecret);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return twitchApiClient;
    }

}
