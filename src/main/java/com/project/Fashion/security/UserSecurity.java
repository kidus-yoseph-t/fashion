package com.project.Fashion.security;

import com.project.Fashion.dto.UserDto;
import com.project.Fashion.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {

    @Autowired
    private UserService userService; // Assuming UserService has a way to get User by email or ID

    public boolean isOwner(Authentication authentication, String userId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            String authenticatedUsername = ((UserDetails) principal).getUsername(); // This is email
            // Need to fetch the User by email to get their ID, or fetch user by ID and compare email
            // This assumes your UserDto or User model has an ID that can be compared to userId
            // For simplicity, if your UserDetails' username IS the User ID (which is a String UUID):
            // UserDto authenticatedUser = userService.getUserByEmail(authenticatedUsername); // You'd need this method
            // return authenticatedUser != null && authenticatedUser.getId().equals(userId);
            // If your User ID is what you store as username in UserDetails:
            // Directly compare. However, we used email as username. So, fetch by ID and compare email.
            try {
                UserDto targetUser = userService.getUser(userId); // getUser by ID
                return targetUser.getEmail().equals(authenticatedUsername);
            } catch (Exception e) {
                return false;
            }
        } else if (principal instanceof String) { // If principal is just the username string
            try {
                UserDto targetUser = userService.getUser(userId); // getUser by ID
                return targetUser.getEmail().equals(principal);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}