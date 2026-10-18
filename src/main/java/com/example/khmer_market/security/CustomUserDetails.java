package com.example.khmer_market.security;

import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.entity.UserSecurity;
import com.example.khmer_market.domain.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final User user;
    private final UserSecurity userSecurity;

    public CustomUserDetails(User user) {
        this(user, user != null ? user.getUserSecurity() : null);
    }

    public CustomUserDetails(User user, UserSecurity userSecurity) {
        this.user = user;
        this.userSecurity = userSecurity;
    }

    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getRoleName() {
        return user.getRole().name();
    }

    public String getFullName() {
        return user.getFullName() != null ? user.getFullName() : user.getUsername();
    }

    public UserSecurity getUserSecurity() {
        return userSecurity;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return userSecurity == null || !userSecurity.isAccountLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() != UserStatus.SUSPENDED;
    }
}
