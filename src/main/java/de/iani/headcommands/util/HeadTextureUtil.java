package de.iani.headcommands.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HeadTextureUtil {
    private static final String TEXTURE_URL_PREFIX = "https://textures.minecraft.net/texture/";

    private HeadTextureUtil() {
        throw new UnsupportedOperationException();
    }

    public static String createTextureValue(String textureUrl) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + TEXTURE_URL_PREFIX + textureUrl + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
