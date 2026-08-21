package org.example.pft.security;

import org.example.pft.entity.Role;
import org.example.pft.entity.User;
import org.example.pft.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(()->new UsernameNotFoundException("User not found"));

        Set<GrantedAuthority> granted = new HashSet<>();

//        Add Role
        if(user.getRoles() != null){
            for(Role role : user.getRoles()){
                if(role.getName() != null){
                    granted.add(new SimpleGrantedAuthority("ROLE_"+role.getName()));
                }
            }
        }
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                granted
        );
    }
}
