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

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserDto register(UserSignUpDto dto) {
        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.password));
        userRepository.save(user);
        return userMapper.toDto(user);
    }

//    public String login(UserSigninDTO dto) {
//        User user = userRepository.findByEmail(dto.email).orElseThrow(() -> new RuntimeException("Invalid email"));
//        if (!passwordEncoder.matches(dto.password, user.getPassword())) {
//            throw new RuntimeException("Invalid password");
//        }
//        return jwtUtil.generateToken(user);
//    }
    public UserDto login(UserSignInDto dto) {
        User user = userRepository.findByEmail(dto.email)
                .orElseThrow(() -> new RuntimeException("Invalid email"));

        if (!passwordEncoder.matches(dto.password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return userMapper.toDto(user);
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
            user.setPassword((String) passwordEncoder.encode((CharSequence) updates.get("password")));
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
