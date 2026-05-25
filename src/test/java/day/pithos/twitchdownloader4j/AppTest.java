package day.pithos.twitchdownloader4j;

import day.pithos.twitchdownloader4j.data.ClipRange;
import day.pithos.twitchdownloader4j.data.Vod;
import day.pithos.twitchdownloader4j.modules.CommentDownloader;
import day.pithos.twitchdownloader4j.modules.DvrDownloader;
import day.pithos.twitchdownloader4j.modules.TwitchApiClient;
import junit.framework.TestCase;
import okhttp3.OkHttpClient;

import java.io.IOException;

public class AppTest extends TestCase {

    private OkHttpClient httpClient;
    private TwitchApiClient twitchApiClient;
    private final String clientId = System.getenv("TWITCH_CLIENT_ID");
    private final String clientSecret = System.getenv("TWITCH_CLIENT_SECRET");

    private final String vodUrl = System.getenv("TWITCH_VOD_URL");

    public void testDownloadDvr() {
        twitchApiClient = getTwitchApiClient();
        try {
            Vod vod = twitchApiClient.getVodData(vodUrl);
            System.out.println(vod.toString());

            DvrDownloader dvrDownloader = new DvrDownloader(getHttpClient(), vod, ClipRange.withDuration(0, 5));

            String dvrPath = "test/" + dvrDownloader.getDefaultFileName();
            dvrDownloader.downloadToFile(dvrPath);
            System.out.println("Dvr has been successfully downloaded on \"" + dvrPath + "\"");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testDownloadComments() {
        twitchApiClient = getTwitchApiClient();
        try {
            Vod vod = twitchApiClient.getVodData(vodUrl);
            System.out.println(vod.toString());

            CommentDownloader commentDownloader = new CommentDownloader(getHttpClient(), vod, ClipRange.withDuration(0, 5));

            String commentPath = "test/" + commentDownloader.getDefaultFileName();
            commentDownloader.downloadToFile(commentPath);
            System.out.println("Comments has been successfully downloaded on \"" + commentPath + "\"");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TwitchApiClient getTwitchApiClient() {
        if (this.twitchApiClient == null) {
            try {
                this.twitchApiClient = new TwitchApiClient(getHttpClient(), clientId, clientSecret);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return this.twitchApiClient;
    }

    private OkHttpClient getHttpClient() {
        if (this.httpClient == null) {
            this.httpClient = new OkHttpClient();
        }
        return this.httpClient;
    }

}
