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

    private final OkHttpClient httpClient;

    public EmoteDownloader(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void downloadToFiles(Set<Comment> comments, Path directory) throws IOException {
        downloadToFiles(comments, directory, false);
    }

    public void downloadToFiles(Set<Comment> comments, Path directory, boolean overwrite) throws IOException {
        Map<String, byte[]> emoteBytesMap = this.fetchEmoteBytesMap(comments);
        Files.createDirectories(directory);

        for (Map.Entry<String, byte[]> entry : emoteBytesMap.entrySet()) {
            String extension = isGif(entry.getValue()) ? ".gif" : ".png";
            Path dst = directory.resolve(entry.getKey() + extension);
            this.saveEmoteBytes(entry.getValue(), dst, overwrite);
        }
    }

    public void downloadToFile(String emoteId, Path dst) throws IOException {
        downloadToFile(emoteId, dst, false);
    }

    public void downloadToFile(String emoteId, Path dst, boolean overwrite) throws IOException {
        byte[] emoteBytes = this.fetchEmoteBytes(emoteId);
        this.saveEmoteBytes(emoteBytes, dst, overwrite);
    }

    private void saveEmoteBytes(byte[] imageBytes, Path dst, boolean overwrite) throws IOException {
        if (!overwrite && Files.exists(dst)) {
            return;
        }

        Files.write(dst, imageBytes);
    }

    public Map<String, byte[]> fetchEmoteBytesMap(Set<Comment> comments) throws IOException {
        Map<String, byte[]> emoteBytesMap = new HashMap<>();

        Set<String> emoteIds = getEmoteIds(comments);

        for (String emoteId : emoteIds) {
            emoteBytesMap.put(emoteId, fetchEmoteBytes(emoteId));
        }

        return emoteBytesMap;
    }

    public byte[] fetchEmoteBytes(String emoteId) throws IOException {

        Request request = new Request.Builder()
                .url("https://static-cdn.jtvnw.net/emoticons/v2/" + emoteId + "/default/dark/2.0")
                .build();

        try (Response res = this.httpClient.newCall(request).execute()) {
            if (!res.isSuccessful()) {
                throw new IOException("Failed! " + res);
            }
            byte[] emoteBytes = res.body().bytes();
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
