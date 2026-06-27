package day.pithos.twitchdownloader4j.config;

public final class TwitchConfig {

    private TwitchConfig() {}

    // --- GQL Client IDs (リクエスト種別ごとに異なる) ---

    public static final String GQL_CLIENT_ID_PLAYBACK = "kimne78kx3ncx6brgo4mv6wki5h1ko";
    public static final String GQL_CLIENT_ID_COMMENTS = "kd1unb4b3q4t58fwlpcbzcbnm76a8fp";

    // --- GQL Persisted Query Hashes ---

    public static final String PLAYBACK_QUERY_HASH = "ed230aa1e33e07eebb8928504583da78a5173989fadfb1ac94be06a04f3cdbe9";
    public static final String COMMENTS_QUERY_HASH = "b70a3591ff0f4e0313d126c6a1502d79a1c02baebb288227c582044aa76adf6a";

    // --- API Base URLs ---

    public static final String GQL_URL = "https://gql.twitch.tv/gql";
    public static final String USHER_VOD_URL = "https://usher.ttvnw.net/vod/v2/";
    public static final String HELIX_VIDEOS_URL = "https://api.twitch.tv/helix/videos";
    public static final String OAUTH_TOKEN_URL = "https://id.twitch.tv/oauth2/token";

    // --- CDN URLs ---

    public static final String EMOTE_CDN_URL = "https://static-cdn.jtvnw.net/emoticons/v2/";

    // --- Other Constants ---

    public static final String APPLICATION_JSON = "application/json";
}
