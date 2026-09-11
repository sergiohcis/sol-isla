package com.hosannasolutions.solisla.security.userdetails;

import com.hosannasolutions.solisla.security.authorization.Role;
import com.hosannasolutions.solisla.security.authorization.RolePermissions;
import com.hosannasolutions.solisla.user.User;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class SolIslaUserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final Role role;
    private final Collection<GrantedAuthority> authorities;

    public SolIslaUserPrincipal(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.enabled = user.isActive();
        this.role = user.getRole();
        this.authorities = RolePermissions.permissionsFor(user.getRole()).stream()
                .map(permission -> (GrantedAuthority) new SimpleGrantedAuthority(permission.name()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public UUID getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
