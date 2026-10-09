package com.example.examseating.config;

import com.example.examseating.entity.Role;
import com.example.examseating.entity.User;
import com.example.examseating.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Seed default development test ADMIN user
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }

        // Seed default development test STUDENT user
        if (!userRepository.existsByUsername("student")) {
            User student = new User();
            student.setUsername("student");
            student.setPassword(passwordEncoder.encode("Student@123"));
            student.setRole(Role.STUDENT);
            userRepository.save(student);
        }
    }
}
