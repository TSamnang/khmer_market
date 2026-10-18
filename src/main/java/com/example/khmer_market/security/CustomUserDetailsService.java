package com.example.khmer_market.security;

import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.entity.UserSecurity;
import com.example.khmer_market.repository.UserRepository;
import com.example.khmer_market.repository.UserSecurityRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserSecurityRepository userSecurityRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserSecurityRepository userSecurityRepository) {
        this.userRepository = userRepository;
        this.userSecurityRepository = userSecurityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        UserSecurity security = userSecurityRepository.findByUserId(user.getId())
                .orElseGet(() -> new UserSecurity(user));

        return new CustomUserDetails(user, security);
    }
}
