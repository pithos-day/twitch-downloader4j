package day.pithos.twitchdownloader4j.modules;

import day.pithos.twitchdownloader4j.config.TwitchConfig;
import day.pithos.twitchdownloader4j.data.Vod;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VodUrlResolver {

    private static final Pattern CF_VODS_PATTERN = Pattern.compile("cf_vods/([^/]+)/([^/]+)/");

    private final TwitchApiClient apiClient;
    private final Vod vod;

    public VodUrlResolver(TwitchApiClient apiClient, Vod vod) {
        this.apiClient = apiClient;
        this.vod = vod;
    }

    public String resolveM3u8Url() throws IOException {
        Matcher matcher = CF_VODS_PATTERN.matcher(this.vod.thumbnailUrl());
        if (matcher.find()) {
            String server = matcher.group(1);
            String directory = matcher.group(2);
            return "https://" + server + ".cloudfront.net/" + directory + "/chunked/index-dvr.m3u8";
        } else {
            return resolveM3u8UrlViaApi();
        }
    }

    private String resolveM3u8UrlViaApi() throws IOException {
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

    static String extractUrlByResolution(String m3u8, String target) throws IOException {
        String[] lines = m3u8.split("\n");

        for (String line : lines) {
            if (line.startsWith("http") && line.contains(target)) {
                return line.trim();
            }
        }
        throw new IOException("No URL found for resolution: " + target);
    }

    record PlaybackAccessToken(String signature, String value) {}

    private PlaybackAccessToken getPlaybackAccessToken() throws IOException {
        Request request = new Request.Builder().url(TwitchConfig.GQL_URL)
                .addHeader("Client-Id", TwitchConfig.GQL_CLIENT_ID_PLAYBACK)
                .post(createVodGqlRequestBody()).build();

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
