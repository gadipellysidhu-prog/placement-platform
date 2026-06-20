package com.college.placement.modules.auth.dto;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    long accessTokenExpiresIn,
    String tokenType
) {
    public static TokenResponse of(String accessToken, String refreshToken, long accessTokenExpiresIn) {
        return new TokenResponse(accessToken, refreshToken, accessTokenExpiresIn, "Bearer");
    }
}
