package com.enrollment.auth.config;

import com.enrollment.auth.model.User;
import com.enrollment.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

@Configuration
public class DataLoader {

    @Bean
    public CommandLineRunner loadData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Only load data if users collection is empty
            if (userRepository.count() == 0) {
                System.out.println("Initializing database with sample users...");

                List<User> users = new ArrayList<>();

                // Create Mary Grace Piattos
                User user1 = new User();
                user1.setId("654f8e3f2b1c4d5e6f7a8b90");
                user1.setFirstName("Mary Grace");
                user1.setLastName("Piattos");
                user1.setEmail("marygrace.piattos@university.edu");
                user1.setPassword(passwordEncoder.encode("password123"));
                user1.setRole("Faculty");
                users.add(user1);

                // Create Fernando Tempura
                User user2 = new User();
                user2.setId("654f8e3f2b1c4d5e6f7a8b91");
                user2.setFirstName("Fernando");
                user2.setLastName("Tempura");
                user2.setEmail("fernando.tempura@university.edu");
                user2.setPassword(passwordEncoder.encode("password123"));
                user2.setRole("Faculty");
                users.add(user2);

                // Create Carlos Miguel Oishi
                User user3 = new User();
                user3.setId("654f8e3f2b1c4d5e6f7a8b92");
                user3.setFirstName("Carlos Miguel");
                user3.setLastName("Oishi");
                user3.setEmail("carlos.oishi@university.edu");
                user3.setPassword(passwordEncoder.encode("password123"));
                user3.setRole("Faculty");
                users.add(user3);

                // Create Chippy McDonald
                User user4 = new User();
                user4.setId("654f8e3f2b1c4d5e6f7a8b93");
                user4.setFirstName("Chippy");
                user4.setLastName("McDonald");
                user4.setEmail("chippy.mcdonald@university.edu");
                user4.setPassword(passwordEncoder.encode("password123"));
                user4.setRole("Faculty");
                users.add(user4);

                // Create Dan Rick Otso
                User user5 = new User();
                user5.setId("654f8e3f2b1c4d5e6f7a8b94");
                user5.setFirstName("Dan Rick");
                user5.setLastName("Otso");
                user5.setEmail("danrick.otso@university.edu");
                user5.setPassword(passwordEncoder.encode("password123"));
                user5.setRole("Faculty");
                users.add(user5);

                // Create Alan Rick Otso
                User user6 = new User();
                user6.setId("654f8e3f2b1c4d5e6f7a8b95");
                user6.setFirstName("Alan Rick");
                user6.setLastName("Otso");
                user6.setEmail("alanrick.otso@university.edu");
                user6.setPassword(passwordEncoder.encode("password123"));
                user6.setRole("Faculty");
                users.add(user6);

                // Save all users
                userRepository.saveAll(users);
                System.out.println("Successfully loaded " + users.size() + " faculty users!");
            } else {
                System.out.println("Database already populated, skipping initialization.");
            }
        };
    }
} 