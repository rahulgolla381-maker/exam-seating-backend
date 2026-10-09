package com.example.examseating.service;

import com.example.examseating.dto.LoginRequest;
import com.example.examseating.dto.LoginResponse;
import com.example.examseating.entity.User;
import com.example.examseating.repository.UserRepository;
import com.example.examseating.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password.");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());

        return new LoginResponse(token, user.getUsername(), user.getRole().name());
    }
}
