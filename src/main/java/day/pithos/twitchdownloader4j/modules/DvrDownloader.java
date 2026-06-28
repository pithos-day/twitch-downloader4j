package day.pithos.twitchdownloader4j.modules;

import day.pithos.twitchdownloader4j.data.ClipRange;
import day.pithos.twitchdownloader4j.data.Vod;
import day.pithos.twitchdownloader4j.parse.M3u8Parser;
import okhttp3.Request;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DvrDownloader extends BaseTwitchDownloader {

    private final VodUrlResolver urlResolver;
    private String cachedM3u8Url;

    public DvrDownloader(TwitchApiClient apiClient, Vod vod, ClipRange clipRange) {
        super(apiClient, vod, clipRange);
        this.urlResolver = new VodUrlResolver(apiClient, vod);
    }

    @Override
    protected String getFileExtension() {
        return "ts";
    }

    public void downloadToFile(Path path) throws IOException {
        String m3u8 = this.fetchM3u8();
        List<String> segments = M3u8Parser.parseSegments(m3u8, start, end);
        this.downloadM3u8(segments, path);
    }

    private String fetchM3u8() throws IOException {
        if (cachedM3u8Url == null) {
            cachedM3u8Url = urlResolver.resolveM3u8Url();
        }
        Request request = new Request.Builder().url(cachedM3u8Url).get().build();
        return apiClient.executeAsString(request);
    }

    private void downloadM3u8(List<String> segments, Path path) throws IOException {
        String baseUrl = cachedM3u8Url.substring(0, cachedM3u8Url.lastIndexOf('/')) + "/";

        try (OutputStream os = Files.newOutputStream(path)) {
            for (String seg : segments) {
                downloadSegment(os, baseUrl + seg);
            }
        }
    }

    private void downloadSegment(OutputStream out, String url) throws IOException {
        Request req = new Request.Builder().url(url).build();
        byte[] data = apiClient.executeAsBytes(req);
        out.write(data);
    }
}
