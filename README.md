# twitch-downloader4j

TwitchのアーカイブをダウンロードするためのJavaライブラリ。

## Usage

### Initialize client

```java
OkHttpClient httpClient = new OkHttpClient();
TwitchApiClient client = new TwitchApiClient(httpClient, clientId, clientSecret);
Vod vod = client.getVodData("https://www.twitch.tv/videos/123456789");
```

### Download DVR

```java
DvrDownloader dvrDownloader = new DvrDownloader(httpClient, vod, ClipRange.withDuration(0, 60));
dvrDownloader.downloadToFile(Path.of("output/" + dvrDownloader.getDefaultFileName()));
```

### Download comments

```java
CommentDownloader commentDownloader = new CommentDownloader(httpClient, vod, ClipRange.withDuration(0, 60));
commentDownloader.downloadToFile(Path.of("output/" + commentDownloader.getDefaultFileName()));
```

### Download emotes

```java
Set<Comment> comments = commentDownloader.fetchComments();
EmoteDownloader emoteDownloader = new EmoteDownloader(httpClient);
emoteDownloader.downloadToFiles(comments, Path.of("output/emotes/"));
```
