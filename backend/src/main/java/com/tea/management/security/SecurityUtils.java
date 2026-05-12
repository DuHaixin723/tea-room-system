package com.tea.management.security;

import com.tea.management.domain.enums.RoleType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("未登录或登录信息无效");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        if (principal instanceof String raw) {
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException ignored) {
                // fallthrough
            }
        }
        throw new AccessDeniedException("未登录或登录信息无效");
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        String expected = "ROLE_" + role;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority != null && expected.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public static RoleType currentRole() {
        if (hasRole("ADMIN")) return RoleType.ADMIN;
        if (hasRole("STAFF")) return RoleType.STAFF;
        if (hasRole("USER")) return RoleType.USER;
        throw new AccessDeniedException("未登录或角色信息无效");
    }

    public static void deny(String message) {
        throw new AccessDeniedException(message);
    }
}
