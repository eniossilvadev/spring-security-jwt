package com.buildrun.springsecurityjwt.controller.dto;

import java.util.List;

public record FeedDto(
        List<FeedItemDto> feedItems,
        int page,
        int pageSize,
        int totalPages,
        long totalElements) {
}
