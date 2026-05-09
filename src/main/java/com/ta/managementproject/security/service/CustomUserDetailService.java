package com.ta.managementproject.security.service;
import com.ta.managementproject.entity.User;
import com.ta.managementproject.repository.UserDb;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service("userDetailsServiceImpl")
public class CustomUserDetailService implements UserDetailsService {
    @Autowired
    private UserDb userDb;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user;
        try {
            user = userDb.findByEmail(email);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        Set<GrantedAuthority> grantedAuthoritySet = new HashSet<>();
        grantedAuthoritySet.add(new SimpleGrantedAuthority(user.getRole().getName()));
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), grantedAuthoritySet);
    }
}
