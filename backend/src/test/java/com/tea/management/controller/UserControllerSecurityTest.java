package com.tea.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tea.management.config.SecurityConfig;
import com.tea.management.domain.entity.User;
import com.tea.management.domain.enums.RoleType;
import com.tea.management.service.JwtTokenService;
import com.tea.management.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * UserControllerSecurityTest 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    void listShouldRejectAnonymous() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void listShouldRejectNonAdmin() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listShouldAllowAdmin() throws Exception {
        User user = new User();
        user.setUsername("admin");
        user.setNickname("管理员");
        user.setPhone("13800000000");
        user.setRole(RoleType.ADMIN);
        user.setEnabled(true);
        when(userService.listAll(any()))
                .thenReturn(new PageImpl<>(List.of(user)));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].username").value("admin"));
    }
}
