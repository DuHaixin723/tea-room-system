package com.tea.management.dto.response;

import java.util.List;
/**
 * RecommendationResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record RecommendationResponse(
        List<TeaResponse> recommendedTeas,
        List<TeaRoomResponse> recommendedTeaRooms
) {
}
