package day.pithos.twitchdownloader4j.util;

public final class ImageUtil {

    private static final byte[] GIF_SIGNATURE = {0x47, 0x49, 0x46, 0x38};

    private ImageUtil() {}

    public static boolean isGif(byte[] bytes) {
        return startsWith(bytes, GIF_SIGNATURE);
    }

    private static boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
