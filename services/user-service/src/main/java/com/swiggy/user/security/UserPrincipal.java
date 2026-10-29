package com.swiggy.user.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Authenticated user principal stored in Spring's SecurityContext.
 */
public record UserPrincipal(
        UUID userId,
        String email,
        String role,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {

    public static UserPrincipal of(UUID userId, String email, String role) {
        // DECISION: Map roles with "ROLE_" prefix standard for Spring Security @PreAuthorize("hasRole(...)").
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        return new UserPrincipal(userId, email, role, authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // Stateless token verification does not retain password hash
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
