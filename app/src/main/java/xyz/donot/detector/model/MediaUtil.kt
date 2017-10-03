package xyz.donot.detector.model

import twitter4j.Status
import java.util.regex.Pattern


private val TWITPIC_PATTERN = Pattern.compile("^http://twitpic\\.com/(\\w+)$")
private val TWIPPLE_PATTERN = Pattern.compile("^http://p\\.twipple\\.jp/(\\w+)$")
private val INSTAGRAM_PATTERN = Pattern.compile("^https?://(?:www\\.)?instagram\\.com/p/([^/]+)/$")
private val PHOTOZOU_PATTERN = Pattern.compile("^http://photozou\\.jp/photo/show/\\d+/(\\d+)$")
private val IMAGES_PATTERN = Pattern.compile("^https?://.*\\.(png|gif|jpeg|jpg)$")
private val YOUTUBE_PATTERN = Pattern.compile("^https?://(?:www\\.youtube\\.com/watch\\?.*v=|youtu\\.be/)([\\w-]+)")
private val NICONICO_PATTERN = Pattern.compile("^http://(?:www\\.nicovideo\\.jp/watch|nico\\.ms)/sm(\\d+)$")
private val PIXIV_PATTERN = Pattern.compile("^http://www\\.pixiv\\.net/member_illust\\.php.*illust_id=(\\d+)")
private val GYAZO_PATTERN = Pattern.compile("^https?://gyazo\\.com/(\\w+)")
// pic.twitter.com
val PIC_TWITTER_GIF = "https?://pbs\\.twimg\\.com/tweet_video_thumb/[a-zA-Z0-9_\\-]+\\.png"
val PIC_TWITTER_GIF_URL_1 = "tweet_video_thumb"
val PIC_TWITTER_GIF_URL_2 = "png"
val PIC_TWITTER_GIF_REPLACE_1 = "tweet_video"
val PIC_TWITTER_GIF_REPLACE_2 = "mp4"
fun getImageUrls(status: Status): ArrayList<String> {
    val imageUrls = ArrayList<String>()
    for (url in status.urlEntities) {
        val twitpic_matcher = TWITPIC_PATTERN.matcher(url.expandedURL)
        if (twitpic_matcher.find()) {
            imageUrls.add("http://twitpic.com/show/full/" + twitpic_matcher.group(1))
            continue
        }
        val twipple_matcher = TWIPPLE_PATTERN.matcher(url.expandedURL)
        if (twipple_matcher.find()) {
            imageUrls.add("http://p.twpl.jp/show/orig/" + twipple_matcher.group(1))
            continue
        }
        val instagram_matcher = INSTAGRAM_PATTERN.matcher(url.expandedURL)
        if (instagram_matcher.find()) {
            imageUrls.add(url.expandedURL + "media?size=l")
            continue
        }
        val photozou_matcher = PHOTOZOU_PATTERN.matcher(url.expandedURL)
        if (photozou_matcher.find()) {
            imageUrls.add("http://photozou.jp/p/img/" + photozou_matcher.group(1))
            continue
        }
        val youtube_matcher = YOUTUBE_PATTERN.matcher(url.expandedURL)
        if (youtube_matcher.find()) {
            imageUrls.add("http://i.ytimg.com/vi/" + youtube_matcher.group(1) + "/hqdefault.jpg")
            continue
        }
        val niconico_matcher = NICONICO_PATTERN.matcher(url.expandedURL)
        if (niconico_matcher.find()) {
            val id = Integer.valueOf(niconico_matcher.group(1))!!
            val host = id % 4 + 1
            imageUrls.add("http://tn-skr$host.smilevideo.jp/smile?i=$id.L")
            continue
        }
        val pixiv_matcher = PIXIV_PATTERN.matcher(url.expandedURL)
        if (pixiv_matcher.find()) {
            imageUrls.add("http://embed.pixiv.net/decorate.php?illust_id=" + pixiv_matcher.group(1))
            continue
        }
        val gyazo_matcher = GYAZO_PATTERN.matcher(url.expandedURL)
        if (gyazo_matcher.find()) {
            imageUrls.add("https://i.gyazo.com/" + gyazo_matcher.group(1) + ".png")
            continue
        }
        val images_matcher = IMAGES_PATTERN.matcher(url.expandedURL)
        if (images_matcher.find()) {
            imageUrls.add(url.expandedURL)
        }
    }

    if (status.mediaEntities.isNotEmpty()) {
        status.mediaEntities.mapTo(imageUrls) { it.mediaURL }
    } else {
        status.mediaEntities.mapTo(imageUrls) { it.mediaURL }
    }

    return imageUrls
}
