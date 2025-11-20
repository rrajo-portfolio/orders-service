package com.portfolio.orders.security;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class SecurityFacade {

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            String subject = jwtAuthenticationToken.getToken().getSubject();
            if (subject != null && !subject.isBlank()) {
                return UUID.fromString(subject);
            }
        }
        return null;
    }

    public boolean hasAnyAuthority(String... authorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        Set<String> granted = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        return Arrays.stream(authorities).anyMatch(granted::contains);
    }

    public void assertCurrentUser(UUID userId) {
        UUID current = getCurrentUserId();
        if (current == null || !current.equals(userId)) {
            throw new AccessDeniedException("User is not allowed to access this resource");
        }
    }
}
