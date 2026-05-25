package day.pithos.twitchdownloader4j.modules;

import day.pithos.twitchdownloader4j.data.Comment;
import day.pithos.twitchdownloader4j.data.comment.EmoteFragment;
import day.pithos.twitchdownloader4j.data.comment.Fragment;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EmoteDownloader {

    private Map<String, byte[]> emoteBytesMap;

    private final OkHttpClient httpClient;

    public EmoteDownloader(OkHttpClient httpClient) {
        this.emoteBytesMap = new HashMap<>();
        this.httpClient = httpClient;
    }

    public void downloadToFiles(Set<Comment> comments, Path directory) throws IOException {
        this.downloadToFiles(comments, directory, false);
    }

    public void downloadToFiles(Set<Comment> comments, Path directory, boolean force) throws IOException {
        Map<String, byte[]> emoteBytesMap = this.fetchEmoteBytesMap(comments, force);
        Files.createDirectories(directory);

        for (Map.Entry<String, byte[]> entry : emoteBytesMap.entrySet()) {
            String extension = isGif(entry.getValue()) ? ".gif" : ".png";
            Path dst = directory.resolve(entry.getKey() + extension);
            this.saveEmoteBytes(entry.getValue(), dst);
        }
    }

    public void downloadToFile(String emoteId, Path dst) throws IOException {
        this.downloadToFile(emoteId, dst, false);
    }

    public void downloadToFile(String emoteId, Path dst, boolean force) throws IOException {
        byte[] emoteBytes = this.fetchEmoteBytes(emoteId, force);
        this.saveEmoteBytes(emoteBytes, dst);
    }

    private void saveEmoteBytes(byte[] imageBytes, Path dst) throws IOException {
        Files.write(dst, imageBytes);
    }

    public Map<String, byte[]> fetchEmoteBytesMap(Set<Comment> comments) throws IOException {
        return this.fetchEmoteBytesMap(comments, false);
    }

    public Map<String, byte[]> fetchEmoteBytesMap(Set<Comment> comments, boolean force) throws IOException {
        Map<String, byte[]> emoteBytesMap = new HashMap<>();

        Set<String> emoteIds = getEmoteIds(comments);

        for (String emoteId : emoteIds) {
            emoteBytesMap.put(emoteId, fetchEmoteBytes(emoteId, force));
        }

        return emoteBytesMap;
    }

    public byte[] fetchEmoteBytes(String emoteId) throws IOException {
        return this.fetchEmoteBytes(emoteId, false);
    }

    public byte[] fetchEmoteBytes(String emoteId, boolean force) throws IOException {
        if (!force && this.emoteBytesMap.containsKey(emoteId)) return emoteBytesMap.get(emoteId);

        Request request = new Request.Builder()
                .url("https://static-cdn.jtvnw.net/emoticons/v2/" + emoteId + "/default/dark/2.0")
                .build();

        try (Response res = this.httpClient.newCall(request).execute()) {
            if (!res.isSuccessful()) {
                throw new IOException("Failed! " + res);
            }
            byte[] emoteBytes = res.body().bytes();
            this.emoteBytesMap.put(emoteId, emoteBytes);
            return emoteBytes;
        } catch (IOException e) {
            throw new IOException("Failed to fetch Emote", e);
        }
    }

    private static Set<String> getEmoteIds(Set<Comment> comments) {
        Set<String> emoteIds = new HashSet<>();

        for (Comment comment : comments) {
            for (Fragment fragment : comment.fragments()) {
                if (fragment instanceof EmoteFragment e) {
                    emoteIds.add(e.emoteId());
                }
            }
        }

        return emoteIds;
    }

    public static boolean isGif(byte[] bytes) {
        return startsWith(bytes, new byte[]{0x47, 0x49, 0x46, 0x38});
    }

    private static boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

}
