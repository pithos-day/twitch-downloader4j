package day.pithos.twitchdownloader4j.modules;

import day.pithos.twitchdownloader4j.config.TwitchConfig;
import day.pithos.twitchdownloader4j.data.ClipRange;
import day.pithos.twitchdownloader4j.data.Vod;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DvrDownloader {

    private final OkHttpClient httpClient;
    private final Vod vod;
    private final int start;
    private final int end;
    private String dvrUrl;

    public DvrDownloader(OkHttpClient httpClient, Vod vod, ClipRange clipRange) {
        if (!clipRange.isValidFor(vod)) throw new IllegalArgumentException("Invalid Clip Range!");
        this.httpClient = httpClient;
        this.vod = vod;
        this.start = clipRange.start();
        this.end = clipRange.end();
    }

    public void downloadToFile(Path path) throws IOException {
        String m3u8 = this.fetchM3u8();
        List<String> segments = this.parseM3u8(m3u8);
        this.downloadM3u8(segments, path);
    }

    public String getDefaultFileName() {
        return this.vod.user().name() + "_" + this.vod.id() + "_" + this.start + "_to_" + this.end + ".ts";
    }

    private String fetchM3u8() throws IOException {
        Request request = new Request.Builder().url(this.getDvrUrl()).get().build();
        try (Response res = execute(request)) {
            return res.body().string();
        }
    }

    private List<String> parseM3u8(String m3u8) {
        String[] lines = m3u8.split("\n");
        double current = 0;
        List<String> result = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].startsWith("#EXTINF:")) continue;

            double duration = parseDuration(lines[i]);
            double segStart = current;
            double segEnd = current + duration;

            if (segEnd <= start) {
                current = segEnd;
                continue;
            }

            if (end <= segStart) break;
            result.add(lines[i + 1]);
            current = segEnd;
        }
        return result;
    }

    private double parseDuration(String line) {
        try {
            return Double.parseDouble(line.replace("#EXTINF:", "").replace(",", ""));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid EXTINF: " + line, e);
        }
    }

    private void downloadM3u8(List<String> segments, Path path) throws IOException {
        OutputStream os = Files.newOutputStream(path);
        String dvrUrl = this.getDvrUrl();
        String baseUrl = dvrUrl.substring(0, dvrUrl.lastIndexOf('/')) + "/";

        for (String seg : segments) {
            downloadSegment(os, baseUrl + seg);
        }

        os.close();
    }

    private void downloadSegment(OutputStream out, String url) throws IOException {
        Request req = new Request.Builder().url(url).build();

        try (Response res = execute(req)) {
            out.write(res.body().bytes());
        }
    }

    private Response execute(Request request) throws IOException {
        Response res = httpClient.newCall(request).execute();

        if (!res.isSuccessful()) {
            throw new IOException("Failed! " + res);
        }
        return res;
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

        try (Response response = this.execute(request)) {
            String m3u8 = response.body().string();
            return extractUrlByResolution(m3u8, "chunked");
        }
    }

    private static String extractUrlByResolution(String m3u8, String target) {
        String[] lines = m3u8.split("\n");

        for (String line : lines) {
            if (line.startsWith("http") && line.contains(target)) {
                return line.trim();
            }
        }
        return null;
    }

    private record PlaybackAccessToken(String signature, String value) {}

    private PlaybackAccessToken getPlaybackAccessToken() throws IOException {
        Request request = new Request.Builder().url(TwitchConfig.GQL_URL)
                .addHeader("Client-Id", TwitchConfig.GQL_CLIENT_ID_PLAYBACK)
                .post(this.createVodGqlRequestBody()).build();

        String signature;
        String value;


        try (Response res = this.httpClient.newCall(request).execute()) {
            if (!res.isSuccessful()) {
                throw new IOException("Failed! " + res);
            }

            JSONObject token = new JSONArray(res.body().string())
                    .getJSONObject(0).getJSONObject("data").getJSONObject("videoPlaybackAccessToken");
            signature = token.getString("signature");
            value = token.getString("value");
        }

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