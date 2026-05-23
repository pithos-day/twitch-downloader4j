package day.pithos.twitchdownloader4j.data;

import day.pithos.twitchdownloader4j.util.DurationUtil;
import org.json.JSONObject;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Vod(String id, String streamId, User user,
                  String title, String description, Instant created, Instant published,
                  String url, String thumbnailUrl, String viewable, int viewCount,
                  String language, String type, int duration) {

    public static Vod fromJsonObject(JSONObject jsonObject) {
        JSONObject data = jsonObject.getJSONArray("data").getJSONObject(0);
        return new Vod(data.getString("id"), data.getString("stream_id"),
                new User(data.getString("user_id"), data.getString("user_login"), data.getString("user_name")),
                data.getString("title"), data.getString("description"),
                Instant.parse(data.getString("created_at")), Instant.parse(data.getString("published_at")),
                data.getString("url"), data.getString("thumbnail_url"),
                data.getString("viewable"), data.getInt("view_count"), data.getString("language"), data.getString("type"),
                DurationUtil.parseToSeconds(data.getString("duration")));
    }

}