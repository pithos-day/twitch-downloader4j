package day.pithos.twitchdownloader4j.data;

public record ClipRange(int start, int end) {

    public static ClipRange withDuration(int start, int duration) {
        return new ClipRange(start, start + duration);
    }

    public boolean isValidFor(Vod vod) {
        return 0 <= start && start < end && end <= vod.duration();
    }

}
