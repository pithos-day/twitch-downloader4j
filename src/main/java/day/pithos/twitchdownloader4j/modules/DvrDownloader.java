package day.pithos.twitchdownloader4j.modules;

import day.pithos.twitchdownloader4j.data.ClipRange;
import day.pithos.twitchdownloader4j.data.Vod;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DvrDownloader {

    private final OkHttpClient httpClient;
    private final Vod vod;
    private final int start;
    private final int end;

    public DvrDownloader(OkHttpClient httpClient, Vod vod, ClipRange clipRange) {
        if (!clipRange.isValidFor(vod)) throw new IllegalArgumentException("Invalid Clip Range!");
        this.httpClient = httpClient;
        this.vod = vod;
        this.start = clipRange.start();
        this.end = clipRange.end();
    }

    public void downloadToFile(String path) throws IOException {
        this.downloadToFile(Path.of(path));
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
        Request request = new Request.Builder().url(this.vod.getDvrUrl()).get().build();
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
        String baseUrl = vod.getDvrUrl().substring(0, vod.getDvrUrl().lastIndexOf('/')) + "/";

        for (String seg : segments) {
            downloadSegment(os, baseUrl + seg);
        }
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
            throw new RuntimeException("Failed! " + res);
        }
        return res;
    }
}