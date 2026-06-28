package day.pithos.twitchdownloader4j.modules;

import day.pithos.twitchdownloader4j.config.TwitchConfig;
import day.pithos.twitchdownloader4j.data.Vod;
import okhttp3.Request;

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
        TwitchApiClient.PlaybackAccessToken token = apiClient.fetchPlaybackAccessToken(this.vod.id());
        String url = TwitchConfig.USHER_VOD_URL + this.vod.id() + ".m3u8" +
                "?supported_codecs=av1,h264" +
                "&transcode_mode=cbr_v1" +
                "&sig=" + token.signature() +
                "&token=" + token.value() +
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
}
