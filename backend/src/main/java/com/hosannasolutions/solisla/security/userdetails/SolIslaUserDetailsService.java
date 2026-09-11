package com.hosannasolutions.solisla.security.userdetails;

import com.hosannasolutions.solisla.user.User;
import com.hosannasolutions.solisla.user.providedService.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SolIslaUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public SolIslaUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email: " + email));
        return new SolIslaUserPrincipal(user);
    }
}
