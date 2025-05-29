package com.project.Fashion.security;

import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.model.User; // Your User entity class
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections; // Import Collections
import java.util.stream.Collectors; // Still needed if you had multiple roles, but good to keep in imports

@Service // Mark this as a Spring component
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // CORRECTED SECTION:
        // Assuming user.getRole() returns a String (e.g., "BUYER", "SELLER", "ADMIN")
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), // Use user.getEmail() here as the unique identifier for Spring Security
                user.getPassword(), // This should be the encoded password from your database
                // Create a list containing a single SimpleGrantedAuthority for the user's role
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}