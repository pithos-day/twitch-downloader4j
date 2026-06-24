package day.pithos.twitchdownloader4j.util;

import day.pithos.twitchdownloader4j.data.Comment;
import day.pithos.twitchdownloader4j.data.User;
import day.pithos.twitchdownloader4j.data.comment.EmoteFragment;
import day.pithos.twitchdownloader4j.data.comment.Fragment;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.util.List;

public class JsonUtil {

    public static JSONObject commentToJson(Comment comment) {
        JSONObject obj = new JSONObject();
        obj.put("id", comment.id());
        obj.put("user", userToJson(comment.user()));
        obj.put("userColor", colorToHex(comment.userColor()));
        obj.put("fragments", fragmentsToJson(comment.fragments()));
        obj.put("contentOffsetSeconds", comment.contentOffsetSeconds());
        obj.put("timeStamp", comment.timeStamp().toString()); // ISO-8601
        return obj;
    }

    public static JSONObject userToJson(User user) {
        JSONObject obj = new JSONObject();
        if (user != null) {
            obj.put("id", user.id());
            obj.put("name", user.name());
            obj.put("displayName", user.displayName());
        }
        return obj;
    }

    public static JSONArray fragmentsToJson(List<Fragment> fragments) {
        JSONArray array = new JSONArray();
        for (Fragment fragment : fragments) {
            array.put(fragmentToJson(fragment));
        }
        return array;
    }

    public static JSONObject fragmentToJson(Fragment fragment) {
        JSONObject obj = new JSONObject();
        obj.put("text", fragment.text());

        if (fragment instanceof EmoteFragment emote) {
            obj.put("type", "emote");
            obj.put("emoteId", emote.emoteId());
        } else {
            obj.put("type", "text");
        }

        return obj;
    }

    public static String colorToHex(Color color) {
        if (color == null) return null;
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
