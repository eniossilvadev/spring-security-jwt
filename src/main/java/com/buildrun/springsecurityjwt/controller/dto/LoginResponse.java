package com.buildrun.springsecurityjwt.controller.dto;

public record LoginResponse(String accessToken, long expiresIn) {
}
