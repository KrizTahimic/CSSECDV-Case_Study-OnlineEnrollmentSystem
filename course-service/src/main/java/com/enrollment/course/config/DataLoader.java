package com.enrollment.course.config;

import com.enrollment.course.model.Course;
import com.enrollment.course.model.Schedule;
import com.enrollment.course.repository.CourseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DataLoader {

    @Bean
    public CommandLineRunner loadData(CourseRepository courseRepository) {
        return args -> {
            // Only load data if courses collection is empty
            if (courseRepository.count() == 0) {
                System.out.println("Initializing database with sample courses...");

                List<Course> courses = new ArrayList<>();

                // Valorant Basics
                Course course1 = new Course();
                course1.setCode("VAL101");
                course1.setTitle("Valorant Basics");
                course1.setDescription("Learn the fundamentals of Valorant, including agent abilities, map strategies, and team composition.");
                course1.setCredits(3);
                course1.setCapacity(30);
                course1.setEnrolled(0);
                
                Schedule schedule1 = new Schedule();
                schedule1.setDays(Arrays.asList("Monday", "Wednesday", "Friday"));
                schedule1.setStartTime("9:00 AM");
                schedule1.setEndTime("10:30 AM");
                schedule1.setRoom("Room 101");
                course1.setSchedule(schedule1);
                
                course1.setStatus("open");
                course1.setInstructorId("654f8e3f2b1c4d5e6f7a8b90"); // Mary Grace Piattos
                courses.add(course1);

                // Marvel Rivals 101
                Course course2 = new Course();
                course2.setCode("MAR101");
                course2.setTitle("Marvel Rivals 101");
                course2.setDescription("Introduction to Marvel Rivals, covering hero mechanics, team synergies, and competitive strategies.");
                course2.setCredits(3);
                course2.setCapacity(25);
                course2.setEnrolled(0);
                
                Schedule schedule2 = new Schedule();
                schedule2.setDays(Arrays.asList("Tuesday", "Thursday"));
                schedule2.setStartTime("1:00 PM");
                schedule2.setEndTime("2:30 PM");
                schedule2.setRoom("Room 102");
                course2.setSchedule(schedule2);
                
                course2.setStatus("open");
                course2.setInstructorId("654f8e3f2b1c4d5e6f7a8b91"); // Fernando Tempura
                courses.add(course2);

                // Apex Legends Academy
                Course course3 = new Course();
                course3.setCode("APX101");
                course3.setTitle("Apex Legends Academy");
                course3.setDescription("Master the art of Apex Legends, from legend abilities to advanced movement techniques.");
                course3.setCredits(3);
                course3.setCapacity(35);
                course3.setEnrolled(0);
                
                Schedule schedule3 = new Schedule();
                schedule3.setDays(Arrays.asList("Monday", "Wednesday", "Friday"));
                schedule3.setStartTime("2:00 PM");
                schedule3.setEndTime("3:30 PM");
                schedule3.setRoom("Room 103");
                course3.setSchedule(schedule3);
                
                course3.setStatus("open");
                course3.setInstructorId("654f8e3f2b1c4d5e6f7a8b92"); // Carlos Miguel Oishi
                courses.add(course3);

                // Genshin Impact Class
                Course course4 = new Course();
                course4.setCode("GEN101");
                course4.setTitle("Genshin Impact Class");
                course4.setDescription("Explore the world of Teyvat, learn about character building, and team compositions.");
                course4.setCredits(3);
                course4.setCapacity(40);
                course4.setEnrolled(0);
                
                Schedule schedule4 = new Schedule();
                schedule4.setDays(Arrays.asList("Tuesday", "Thursday"));
                schedule4.setStartTime("9:00 AM");
                schedule4.setEndTime("10:30 AM");
                schedule4.setRoom("Room 104");
                course4.setSchedule(schedule4);
                
                course4.setStatus("open");
                course4.setInstructorId("654f8e3f2b1c4d5e6f7a8b93"); // Chippy McDonald
                courses.add(course4);

                // Introduction to Palworld
                Course course5 = new Course();
                course5.setCode("PAL101");
                course5.setTitle("Introduction to Palworld");
                course5.setDescription("Learn about Pal management, base building, and survival strategies in Palworld.");
                course5.setCredits(3);
                course5.setCapacity(30);
                course5.setEnrolled(0);
                
                Schedule schedule5 = new Schedule();
                schedule5.setDays(Arrays.asList("Monday", "Wednesday", "Friday"));
                schedule5.setStartTime("1:00 PM");
                schedule5.setEndTime("2:30 PM");
                schedule5.setRoom("Room 105");
                course5.setSchedule(schedule5);
                
                course5.setStatus("open");
                course5.setInstructorId("654f8e3f2b1c4d5e6f7a8b94"); // Dan Rick Otso
                courses.add(course5);

                // Pokemon School
                Course course6 = new Course();
                course6.setCode("PKM101");
                course6.setTitle("Pokemon School");
                course6.setDescription("Master Pokemon battling, team building, and competitive strategies.");
                course6.setCredits(3);
                course6.setCapacity(35);
                course6.setEnrolled(0);
                
                Schedule schedule6 = new Schedule();
                schedule6.setDays(Arrays.asList("Tuesday", "Thursday"));
                schedule6.setStartTime("2:00 PM");
                schedule6.setEndTime("3:30 PM");
                schedule6.setRoom("Room 106");
                course6.setSchedule(schedule6);
                
                course6.setStatus("open");
                course6.setInstructorId("654f8e3f2b1c4d5e6f7a8b95"); // Alan Rick Otso
                courses.add(course6);

                // Save all courses
                courseRepository.saveAll(courses);
                System.out.println("Successfully loaded " + courses.size() + " courses!");
            } else {
                System.out.println("Course database already populated, skipping initialization.");
            }
        };
    }
} 