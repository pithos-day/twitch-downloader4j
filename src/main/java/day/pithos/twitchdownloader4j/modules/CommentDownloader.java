package day.pithos.twitchdownloader4j.modules;

import day.pithos.twitchdownloader4j.data.ClipRange;
import day.pithos.twitchdownloader4j.data.Comment;
import day.pithos.twitchdownloader4j.data.Vod;
import day.pithos.twitchdownloader4j.parse.CommentParser;
import day.pithos.twitchdownloader4j.util.JsonUtil;
import okhttp3.Request;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class CommentDownloader extends BaseTwitchDownloader {

    private static final int COMMENT_TIME_BUFFER = 10;

    private Set<Comment> comments;

    public CommentDownloader(TwitchApiClient apiClient, Vod vod, ClipRange clipRange) {
        super(apiClient, vod, clipRange);
    }

    @Override
    protected String getFileExtension() {
        return "json";
    }

    public void downloadToFile(Path path) throws IOException {
        JSONArray array = new JSONArray();
        Set<Comment> comments = this.fetchComments();

        for (Comment comment : comments) {
            array.put(JsonUtil.commentToJson(comment));
        }

        Files.writeString(path, array.toString(2));
    }

    public Set<Comment> fetchComments() throws IOException {
        return this.fetchComments(false);
    }

    public Set<Comment> fetchComments(boolean force) throws IOException {
        if (!force && this.comments != null) return this.comments;
        Set<Comment> comments = new LinkedHashSet<>();
        String cursor = null;
        int lastTime = 0;

        boolean isFirstPage = true;
        while (isFirstPage || (cursor != null && lastTime <= end + COMMENT_TIME_BUFFER)) {
            JSONObject rawPage = fetchRawPage(cursor);
            CommentParser.Page page = CommentParser.parsePage(rawPage);
            comments.addAll(page.comments());

            lastTime = page.lastTime();
            cursor = page.nextCursor();
            isFirstPage = false;
        }
        return this.comments = comments;
    }

    private JSONObject fetchRawPage(String cursor) throws IOException {
        Request request = apiClient.createCommentGqlRequest(this.vod.id(), start, cursor);
        String body = apiClient.executeAsString(request);
        return new JSONArray(body)
                .getJSONObject(0)
                .getJSONObject("data")
                .getJSONObject("video")
                .getJSONObject("comments");
    }
}
