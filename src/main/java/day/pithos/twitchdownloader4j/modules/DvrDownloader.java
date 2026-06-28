package day.pithos.twitchdownloader4j.modules;

import day.pithos.twitchdownloader4j.config.TwitchConfig;
import day.pithos.twitchdownloader4j.data.ClipRange;
import day.pithos.twitchdownloader4j.data.Vod;
import day.pithos.twitchdownloader4j.parse.M3u8Parser;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DvrDownloader extends BaseTwitchDownloader {

    private String dvrUrl;

    public DvrDownloader(TwitchApiClient apiClient, Vod vod, ClipRange clipRange) {
        super(apiClient, vod, clipRange);
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
        Request request = new Request.Builder().url(this.getDvrUrl()).get().build();
        return apiClient.executeAsString(request);
    }

    private void downloadM3u8(List<String> segments, Path path) throws IOException {
        String dvrUrl = this.getDvrUrl();
        String baseUrl = dvrUrl.substring(0, dvrUrl.lastIndexOf('/')) + "/";

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

    private String getDvrUrl() throws IOException {
        return this.getDvrUrl(false);
    }

    private String getDvrUrl(boolean force) throws IOException {
        if (!force && dvrUrl != null) return this.dvrUrl;

        Pattern pattern = Pattern.compile("cf_vods/([^/]+)/([^/]+)/");
        Matcher matcher = pattern.matcher(this.vod.thumbnailUrl());
        if (matcher.find()) {
            String server = matcher.group(1);
            String directory = matcher.group(2);
            return this.dvrUrl = "https://" + server + ".cloudfront.net/" + directory + "/chunked/index-dvr.m3u8";
        } else {
            return this.dvrUrl = this.getDvrUrlWithRequest();
        }
    }

    private String getDvrUrlWithRequest() throws IOException {
        PlaybackAccessToken playbackAccessToken = getPlaybackAccessToken();
        String url = TwitchConfig.USHER_VOD_URL + this.vod.id() + ".m3u8" +
                "?supported_codecs=av1,h264" +
                "&transcode_mode=cbr_v1" +
                "&sig=" + playbackAccessToken.signature() +
                "&token=" + playbackAccessToken.value() +
                "&allow_source=true";

        Request request = new Request.Builder().url(url).get().build();
        String m3u8 = apiClient.executeAsString(request);
        return extractUrlByResolution(m3u8, "chunked");
    }

    private static String extractUrlByResolution(String m3u8, String target) throws IOException {
        String[] lines = m3u8.split("\n");

        for (String line : lines) {
            if (line.startsWith("http") && line.contains(target)) {
                return line.trim();
            }
        }
        throw new IOException("No URL found for resolution: " + target);
    }

    private record PlaybackAccessToken(String signature, String value) {}

    private PlaybackAccessToken getPlaybackAccessToken() throws IOException {
        Request request = new Request.Builder().url(TwitchConfig.GQL_URL)
                .addHeader("Client-Id", TwitchConfig.GQL_CLIENT_ID_PLAYBACK)
                .post(this.createVodGqlRequestBody()).build();

        String body = apiClient.executeAsString(request);
        JSONObject token = new JSONArray(body)
                .getJSONObject(0).getJSONObject("data").getJSONObject("videoPlaybackAccessToken");
        String signature = token.getString("signature");
        String value = token.getString("value");

        return new PlaybackAccessToken(signature, value);
    }

    private RequestBody createVodGqlRequestBody() {
        JSONObject initialQuery = new JSONObject()
                .put("operationName", "PlaybackAccessToken")
                .put("variables", new JSONObject()
                        .put("isLive", false)
                        .put("isVod", true)
                        .put("login", "")
                        .put("vodID", this.vod.id())
                        .put("platform", "web")
                        .put("playerType", "site"))
                .put("extensions", new JSONObject()
                        .put("persistedQuery", new JSONObject()
                                .put("version", 1)
                                .put("sha256Hash", TwitchConfig.PLAYBACK_QUERY_HASH)));
        JSONArray queryArray = new JSONArray().put(initialQuery);
        return RequestBody.create(queryArray.toString(), MediaType.get(TwitchConfig.APPLICATION_JSON));
    }
}