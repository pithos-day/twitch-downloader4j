package day.pithos.twitchdownloader4j;

import day.pithos.twitchdownloader4j.data.ClipRange;
import day.pithos.twitchdownloader4j.data.Comment;
import day.pithos.twitchdownloader4j.data.Vod;
import day.pithos.twitchdownloader4j.modules.CommentDownloader;
import day.pithos.twitchdownloader4j.modules.DvrDownloader;
import day.pithos.twitchdownloader4j.modules.EmoteDownloader;
import day.pithos.twitchdownloader4j.modules.TwitchApiClient;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class AppTest {

    private OkHttpClient httpClient;
    private TwitchApiClient twitchApiClient;
    private final String clientId = System.getenv("TWITCH_CLIENT_ID");
    private final String clientSecret = System.getenv("TWITCH_CLIENT_SECRET");
    private final String vodUrl = System.getenv("TWITCH_VOD_URL");

    @Test
    public void testDownloadDvr() {
        assertDoesNotThrow(() -> {
            TwitchApiClient client = getTwitchApiClient();
            Vod vod = client.getVodData(vodUrl);
            System.out.println(vod.toString());

            DvrDownloader dvrDownloader = new DvrDownloader(client, vod, ClipRange.withDuration(0, 5));

            Path dvrPath = Path.of("test/" + dvrDownloader.getDefaultFileName());
            dvrDownloader.downloadToFile(dvrPath);
            System.out.println("Dvr has been successfully downloaded on \"" + dvrPath + "\"");
        });
    }

    @Test
    public void testDownloadComments() {
        assertDoesNotThrow(() -> {
            TwitchApiClient client = getTwitchApiClient();
            Vod vod = client.getVodData(vodUrl);
            System.out.println(vod.toString());

            CommentDownloader commentDownloader = new CommentDownloader(client, vod, ClipRange.withDuration(0, 5));

            Path commentPath = Path.of("test/" + commentDownloader.getDefaultFileName());
            commentDownloader.downloadToFile(commentPath);
            System.out.println("Comments has been successfully downloaded on \"" + commentPath + "\"");
        });
    }

    @Test
    public void testDownloadEmotes() {
        assertDoesNotThrow(() -> {
            TwitchApiClient client = getTwitchApiClient();
            Vod vod = client.getVodData(vodUrl);
            System.out.println(vod.toString());

            CommentDownloader commentDownloader = new CommentDownloader(client, vod, ClipRange.withDuration(0, 5));

            Path emotesPath = Path.of("test/emote/");
            EmoteDownloader emoteDownloader = new EmoteDownloader(client);
            Set<Comment> comments = commentDownloader.fetchComments();
            emoteDownloader.downloadToFiles(comments, emotesPath);

            System.out.println("Emotes has been successfully downloaded on \"" + emotesPath + "\"");
        });
    }

    private TwitchApiClient getTwitchApiClient() throws IOException {
        if (this.twitchApiClient == null) {
            this.twitchApiClient = TwitchApiClient.create(getHttpClient(), clientId, clientSecret);
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
