package day.pithos.twitchdownloader4j.modules;

import day.pithos.twitchdownloader4j.config.TwitchConfig;
import day.pithos.twitchdownloader4j.data.ClipRange;
import day.pithos.twitchdownloader4j.data.Comment;
import day.pithos.twitchdownloader4j.data.User;
import day.pithos.twitchdownloader4j.data.Vod;
import day.pithos.twitchdownloader4j.data.comment.EmoteFragment;
import day.pithos.twitchdownloader4j.data.comment.Fragment;
import day.pithos.twitchdownloader4j.data.comment.TextFragment;
import day.pithos.twitchdownloader4j.util.JsonUtil;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CommentDownloader {

    private Set<Comment> comments;
    private final OkHttpClient httpClient;
    private final Vod vod;
    private final int start;
    private final int end;

    public CommentDownloader(OkHttpClient httpClient, Vod vod, ClipRange clipRange) {
        if (!clipRange.isValidFor(vod)) throw new IllegalArgumentException("Invalid Clip Range!");
        this.httpClient = httpClient;
        this.vod = vod;
        this.start = clipRange.start();
        this.end = clipRange.end();
    }

    public void downloadToFile(Path path) throws IOException {
        JSONArray array = new JSONArray();
        Set<Comment> comments = this.fetchComments();

        for (Comment comment : comments) {
            array.put(JsonUtil.commentToJson(comment));
        }

        Files.writeString(path, array.toString(2));
    }

    public String getDefaultFileName() {
        return this.vod.user().name() + "_" + this.vod.id() + "_" + this.start + "_to_" + this.end + ".json";
    }

    public Set<Comment> fetchComments() throws IOException {
        return this.fetchComments(false);
    }

    public Set<Comment> fetchComments(boolean force) throws IOException {
        if (!force && this.comments != null) return this.comments;
        Set<Comment> comments = new LinkedHashSet<>();
        String cursor = null;
        int lastTime = 0;

        while (lastTime <= end + 10 && cursor != null || lastTime == 0) {
            Page page = parsePage(fetchRawPage(cursor));
            comments.addAll(page.comments());

            lastTime = page.lastTime();
            cursor = page.nextCursor();
        }
        return this.comments = comments;
    }

    private JSONObject fetchRawPage(String cursor) throws IOException {
        Request request = new Request.Builder().url(TwitchConfig.GQL_URL)
                .addHeader("Client-Id", TwitchConfig.GQL_CLIENT_ID_COMMENTS)
                .post(this.createChatGqlRequestBody(cursor)).build();

        try (Response res = httpClient.newCall(request).execute()) {
            if (!res.isSuccessful()) {
                throw new IOException("Failed! " + res);
            }

            return new JSONArray(res.body().string())
                    .getJSONObject(0)
                    .getJSONObject("data")
                    .getJSONObject("video")
                    .getJSONObject("comments");
        }
    }

    private Page parsePage(JSONObject json) {
        JSONArray edges = json.getJSONArray("edges");
        List<Comment> comments = new ArrayList<>(edges.length());

        for (int i = 0; i < edges.length(); i++) {
            comments.add(parseComment(edges.getJSONObject(i)));
        }

        JSONObject lastEdge = edges.getJSONObject(edges.length() - 1);
        int lastTime = lastEdge.getJSONObject("node").getInt("contentOffsetSeconds");
        String cursor = json.getJSONObject("pageInfo").getBoolean("hasNextPage") ? lastEdge.getString("cursor") : null;

        return new Page(comments, lastTime, cursor);
    }

    private Comment parseComment(JSONObject edge) {
        JSONObject node = edge.getJSONObject("node");

        User user = parseUser(node.optJSONObject("commenter"));
        String color = node.getJSONObject("message").optString("userColor", null);

        JSONArray rawFragments = node.getJSONObject("message").getJSONArray("fragments");
        List<Fragment> fragments = new ArrayList<>(rawFragments.length());
        for (int j = 0; j < rawFragments.length(); j++) {
            JSONObject fragment = rawFragments.getJSONObject(j);
            String text = fragment.getString("text");
            if (fragment.optJSONObject("emote", null) != null) {
                String emoteId = fragment.getJSONObject("emote").getString("emoteID");
                fragments.add(new EmoteFragment(text, emoteId));
            } else {
                fragments.add(new TextFragment(text));
            }
        }

        return new Comment(
                user,
                color != null ? Color.decode(color) : null,
                fragments,
                node.getInt("contentOffsetSeconds"),
                Instant.parse(node.getString("createdAt")),
                node.getString("id")
        );
    }

    private User parseUser(JSONObject json) {
        if (json == null) return null;

        return new User(
                json.getString("id"),
                json.getString("login"),
                json.getString("displayName")
        );
    }

    private RequestBody createChatGqlRequestBody(String cursor) {
        JSONObject initialQuery = new JSONObject()
                .put("operationName", "VideoCommentsByOffsetOrCursor")
                .put("variables", new JSONObject()
                        .put("videoID", this.vod.id())
                        .put(cursor == null ? "contentOffsetSeconds" : "cursor", cursor == null ? start : cursor))
                .put("extensions", new JSONObject()
                        .put("persistedQuery", new JSONObject()
                                .put("version", 1)
                                .put("sha256Hash", TwitchConfig.COMMENTS_QUERY_HASH)));
        JSONArray queryArray = new JSONArray().put(initialQuery);
        return RequestBody.create(queryArray.toString(), MediaType.get(TwitchConfig.APPLICATION_JSON));
    }

    private record Page(List<Comment> comments, int lastTime, String nextCursor) {}

}

