package day.pithos.twitchdownloader4j.modules;

import day.pithos.twitchdownloader4j.config.TwitchConfig;
import day.pithos.twitchdownloader4j.data.ClipRange;
import day.pithos.twitchdownloader4j.data.Comment;
import day.pithos.twitchdownloader4j.data.Vod;
import day.pithos.twitchdownloader4j.parse.CommentParser;
import day.pithos.twitchdownloader4j.util.JsonUtil;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
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
        Request request = new Request.Builder().url(TwitchConfig.GQL_URL)
                .addHeader("Client-Id", TwitchConfig.GQL_CLIENT_ID_COMMENTS)
                .post(createChatGqlRequestBody(cursor)).build();

        String body = apiClient.executeAsString(request);
        return new JSONArray(body)
                .getJSONObject(0)
                .getJSONObject("data")
                .getJSONObject("video")
                .getJSONObject("comments");
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
}
