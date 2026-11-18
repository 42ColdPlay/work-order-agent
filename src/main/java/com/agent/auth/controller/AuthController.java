package com.agent.auth.controller;

import com.agent.auth.dto.AuthResponse;
import com.agent.auth.dto.LoginRequest;
import com.agent.auth.dto.RegisterRequest;
import com.agent.auth.entity.User;
import com.agent.auth.repository.UserRepository;
import com.agent.auth.security.JwtAuthFilter;
import com.agent.auth.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthFilter jwtAuthFilter;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider,
                          JwtAuthFilter jwtAuthFilter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "用户名已存在"));
        }

        String encodedPwd = passwordEncoder.encode(request.getPassword());
        String displayName = request.getDisplayName() != null ? request.getDisplayName() : request.getUsername();

        User user = new User(request.getUsername(), encodedPwd, displayName);
        user = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getDisplayName(), user.getRole()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "用户名或密码错误"));
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getDisplayName(), user.getRole()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "缺少 Token"));
        }
        String token = authHeader.substring(7);
        jwtAuthFilter.blacklist(token);
        return ResponseEntity.ok(Map.of("message", "退出成功"));
    }
}