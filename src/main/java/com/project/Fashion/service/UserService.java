//package com.project.Fashion.service;
//
//import com.project.Fashion.config.mappers.UserMapper;
//import com.project.Fashion.dto.UserDto;
//import com.project.Fashion.dto.UserSignInDto;
//import com.project.Fashion.dto.UserSignUpDto;
//import com.project.Fashion.model.User;
//import com.project.Fashion.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class UserService {
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final UserMapper userMapper;
//
//    public UserDto register(UserSignUpDto dto) {
//        User user = userMapper.toEntity(dto);
//        user.setPassword(passwordEncoder.encode(dto.password));
//        userRepository.save(user);
//        return userMapper.toDto(user);
//    }
//
////    public String login(UserSigninDTO dto) {
////        User user = userRepository.findByEmail(dto.email).orElseThrow(() -> new RuntimeException("Invalid email"));
////        if (!passwordEncoder.matches(dto.password, user.getPassword())) {
////            throw new RuntimeException("Invalid password");
////        }
////        return jwtUtil.generateToken(user);
////    }
//    public UserDto login(UserSignInDto dto) {
//        User user = userRepository.findByEmail(dto.email)
//                .orElseThrow(() -> new RuntimeException("Invalid email"));
//
//        if (!passwordEncoder.matches(dto.password, user.getPassword())) {
//            throw new RuntimeException("Invalid password");
//        }
//
//        return userMapper.toDto(user);
//    }
//    public List<UserDto> getAllUsers() {
//        return userRepository.findAll().stream().map(userMapper::toDto).toList();
//    }
//
//    public UserDto getUser(String id) {
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//        return  userMapper.toDto(user);
//    }
//
//    public UserDto updateUser(String id, UserSignUpDto dto) {
//        User existing = userRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        existing.setFirstname(dto.firstname);
//        existing.setLastname(dto.lastname);
//        existing.setEmail(dto.email);
//        existing.setRole(dto.role);
//        existing.setPassword(passwordEncoder.encode(dto.password));
//        userRepository.save(existing);
//
//        return userMapper.toDto(existing);
//    }
//
//    public UserDto patchUser(String id, Map<String, Object> updates) {
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        if (updates.containsKey("firstname")) {
//            user.setFirstname((String) updates.get("firstname"));
//        }
//        if (updates.containsKey("lastname")) {
//            user.setLastname((String) updates.get("lastname"));
//        }
//        if (updates.containsKey("email")) {
//            user.setEmail((String) updates.get("email"));
//        }
//        if (updates.containsKey("role")) {
//            user.setRole((String) updates.get("role"));
//        }
//        if (updates.containsKey("password")) {
//            user.setPassword((String) passwordEncoder.encode((CharSequence) updates.get("password")));
//        }
//
//        userRepository.save(user);
//        return userMapper.toDto(user);
//    }
//
//    public void deleteUser(String id) {
//        if (!userRepository.existsById(id)) {
//            throw new RuntimeException("User not found");
//        }
//        userRepository.deleteById(id);
//    }
//
//}

package com.project.Fashion.service;

import com.project.Fashion.config.mappers.UserMapper;
import com.project.Fashion.dto.UserDto;
import com.project.Fashion.dto.UserSignInDto;
import com.project.Fashion.dto.UserSignUpDto;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// *** NEW IMPORTS FOR SPRING SECURITY AUTHENTICATION ***
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
// ******************************************************

@Service
@RequiredArgsConstructor // This will now automatically inject AuthenticationManager if it's a final field
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    // *** NEW: Inject AuthenticationManager ***
    private final AuthenticationManager authenticationManager;

    public UserDto register(UserSignUpDto dto) {
        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.password));
        // You also need to ensure the role is set correctly here from the DTO
        // Example: user.setRole(dto.getRole()); // If your User entity has a setRole(String) method
        // Or if you have a Role enum: user.setRole(Role.valueOf(dto.getRole().toUpperCase()));
        userRepository.save(user);
        return userMapper.toDto(user);
    }

    public UserDto login(UserSignInDto dto) {
        try {
            // 1. Create an authentication token with the provided credentials
            // Assuming email is the 'username' for Spring Security's UserDetailsService
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword());

            // 2. Authenticate the user using the AuthenticationManager
            // This will delegate to your UserDetailsService and PasswordEncoder
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // 3. Set the authenticated principal in the SecurityContextHolder
            // This is the CRITICAL STEP that tells Spring Security:
            // "This user is now logged in. Create a session (JSESSIONID) for them!"
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 4. Optionally, retrieve the full User object again if needed for the DTO response.
            // The 'authentication.getPrincipal()' will return your UserDetails object.
            // If you need the full entity, you might re-fetch from the repository or cast the principal.
            User authenticatedUser = userRepository.findByEmail(dto.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found after successful authentication."));


            return userMapper.toDto(authenticatedUser);

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            // Handle specific Spring Security authentication failures
            throw new RuntimeException("Invalid email or password.", e); // More specific error
        } catch (UsernameNotFoundException e) {
            throw new RuntimeException("User not found.", e); // Should be caught by BadCredentialsException typically
        } catch (Exception e) {
            // Catch any other unexpected authentication exceptions
            throw new RuntimeException("Login failed due to an unexpected error: " + e.getMessage(), e);
        }
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(userMapper::toDto).toList();
    }

    public UserDto getUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return  userMapper.toDto(user);
    }

    public UserDto updateUser(String id, UserSignUpDto dto) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        existing.setFirstname(dto.firstname);
        existing.setLastname(dto.lastname);
        existing.setEmail(dto.email);
        existing.setRole(dto.role);
        // Ensure password is encoded when updating
        existing.setPassword(passwordEncoder.encode(dto.password));
        userRepository.save(existing);

        return userMapper.toDto(existing);
    }

    public UserDto patchUser(String id, Map<String, Object> updates) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updates.containsKey("firstname")) {
            user.setFirstname((String) updates.get("firstname"));
        }
        if (updates.containsKey("lastname")) {
            user.setLastname((String) updates.get("lastname"));
        }
        if (updates.containsKey("email")) {
            user.setEmail((String) updates.get("email"));
        }
        if (updates.containsKey("role")) {
            user.setRole((String) updates.get("role"));
        }
        if (updates.containsKey("password")) {
            // Ensure password is encoded when patching
            user.setPassword(passwordEncoder.encode((String) updates.get("password")));
        }

        userRepository.save(user);
        return userMapper.toDto(user);
    }

    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }
}