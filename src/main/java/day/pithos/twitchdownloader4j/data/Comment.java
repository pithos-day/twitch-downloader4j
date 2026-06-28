package day.pithos.twitchdownloader4j.data;

import day.pithos.twitchdownloader4j.data.comment.EmoteFragment;
import day.pithos.twitchdownloader4j.data.comment.Fragment;

import java.awt.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record Comment(User user, Color userColor,
                      List<Fragment> fragments,
                      int contentOffsetSeconds, Instant timeStamp,
                      String id) {

    public String text() {
        StringBuilder sb = new StringBuilder();
        for (Fragment messageFragment : this.fragments) {
            sb.append(messageFragment.text());
        }
        return sb.toString();
    }

    public Set<String> emoteIds() {
        Set<String> ids = new HashSet<>();
        for (Fragment fragment : this.fragments) {
            if (fragment instanceof EmoteFragment e) {
                ids.add(e.emoteId());
            }
        }
        return ids;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Comment other)) return false;
        if (this == o) return true;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
