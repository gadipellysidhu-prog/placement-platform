package com.college.placement.shared.filepipeline.service;

import com.college.placement.shared.filepipeline.exception.FileStorageException;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class FileHashService {

    private static final String SHA_256 = "SHA-256";

    public String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance(SHA_256).digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new FileStorageException("SHA-256 algorithm unavailable", ex);
        }
    }
}
