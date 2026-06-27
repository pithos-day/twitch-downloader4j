package day.pithos.twitchdownloader4j.modules;

import day.pithos.twitchdownloader4j.data.ClipRange;
import day.pithos.twitchdownloader4j.data.Vod;

public abstract class BaseTwitchDownloader {

    protected final TwitchApiClient apiClient;
    protected final Vod vod;
    protected final int start;
    protected final int end;

    protected BaseTwitchDownloader(TwitchApiClient apiClient, Vod vod, ClipRange clipRange) {
        if (!clipRange.isValidFor(vod)) throw new IllegalArgumentException("Invalid Clip Range!");
        this.apiClient = apiClient;
        this.vod = vod;
        this.start = clipRange.start();
        this.end = clipRange.end();
    }

    public String getDefaultFileName() {
        return this.vod.user().name() + "_" + this.vod.id() + "_" + this.start + "_to_" + this.end + "." + getFileExtension();
    }

    protected abstract String getFileExtension();
}
