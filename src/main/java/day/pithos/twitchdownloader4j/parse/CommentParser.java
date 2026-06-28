package day.pithos.twitchdownloader4j.parse;

import day.pithos.twitchdownloader4j.data.Comment;
import day.pithos.twitchdownloader4j.data.User;
import day.pithos.twitchdownloader4j.data.comment.EmoteFragment;
import day.pithos.twitchdownloader4j.data.comment.Fragment;
import day.pithos.twitchdownloader4j.data.comment.TextFragment;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class CommentParser {

    private CommentParser() {}

    public record Page(List<Comment> comments, int lastTime, String nextCursor) {}

    public static Page parsePage(JSONObject json) {
        JSONArray edges = json.getJSONArray("edges");
        List<Comment> comments = new ArrayList<>(edges.length());

        for (int i = 0; i < edges.length(); i++) {
            comments.add(parseComment(edges.getJSONObject(i)));
        }

        if (edges.length() == 0) {
            return new Page(comments, -1, null);
        }

        JSONObject lastEdge = edges.getJSONObject(edges.length() - 1);
        int lastTime = lastEdge.getJSONObject("node").getInt("contentOffsetSeconds");
        String cursor = json.getJSONObject("pageInfo").getBoolean("hasNextPage") ? lastEdge.getString("cursor") : null;

        return new Page(comments, lastTime, cursor);
    }

    public static Comment parseComment(JSONObject edge) {
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

    private static User parseUser(JSONObject json) {
        if (json == null) return null;

        return new User(
                json.getString("id"),
                json.getString("login"),
                json.getString("displayName")
        );
    }
}
