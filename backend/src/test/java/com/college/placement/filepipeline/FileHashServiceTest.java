package com.college.placement.filepipeline;

import com.college.placement.shared.filepipeline.service.FileHashService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FileHashServiceTest {

    private final FileHashService hashService = new FileHashService();

    @Test
    void sha256Hex_knownInput_producesExpectedDigest() {
        // SHA-256("hello") = 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
        byte[] input = "hello".getBytes(StandardCharsets.UTF_8);
        String hash  = hashService.sha256Hex(input);
        assertThat(hash).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void sha256Hex_sameInput_producesIdenticalDigest() {
        byte[] input = "placement-platform".getBytes(StandardCharsets.UTF_8);
        assertThat(hashService.sha256Hex(input)).isEqualTo(hashService.sha256Hex(input));
    }

    @Test
    void sha256Hex_differentInput_producesDifferentDigest() {
        byte[] a = "abc".getBytes(StandardCharsets.UTF_8);
        byte[] b = "xyz".getBytes(StandardCharsets.UTF_8);
        assertThat(hashService.sha256Hex(a)).isNotEqualTo(hashService.sha256Hex(b));
    }

    @Test
    void sha256Hex_emptyBytes_returnsKnownDigest() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        String hash = hashService.sha256Hex(new byte[0]);
        assertThat(hash).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void sha256Hex_returnsLowercase64HexChars() {
        byte[] input = "test".getBytes(StandardCharsets.UTF_8);
        String hash  = hashService.sha256Hex(input);
        assertThat(hash)
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }
}
