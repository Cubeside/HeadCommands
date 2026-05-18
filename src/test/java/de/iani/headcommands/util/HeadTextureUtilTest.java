package de.iani.headcommands.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class HeadTextureUtilTest {
    @Test
    void createsTextureValueFromShortTextureUrl() {
        String value = HeadTextureUtil.createTextureValue("abcdef");
        String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);

        assertEquals("{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/abcdef\"}}}", decoded);
    }
}
