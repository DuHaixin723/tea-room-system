package com.tea.management.controller;

import com.tea.management.config.SecurityConfig;
import com.tea.management.dto.response.StatisticResponse;
import com.tea.management.service.JwtTokenService;
import com.tea.management.service.StatisticService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * StatisticControllerSecurityTest 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@WebMvcTest(StatisticController.class)
@Import(SecurityConfig.class)
class StatisticControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticService statisticService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void overviewShouldAllowAdmin() throws Exception {
        when(statisticService.overview()).thenReturn(
                new StatisticResponse(10, 2, 1, 5, 8, 12, 3, 6, new BigDecimal("999.00"))
        );

        mockMvc.perform(get("/api/statistics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderCount").value(6))
                .andExpect(jsonPath("$.data.totalOrderAmount").value(999.00));
    }

    @Test
    @WithMockUser(roles = "USER")
    void overviewShouldRejectUser() throws Exception {
        mockMvc.perform(get("/api/statistics/overview"))
                .andExpect(status().isForbidden());
    }
}
