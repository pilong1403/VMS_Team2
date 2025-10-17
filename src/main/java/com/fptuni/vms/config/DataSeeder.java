package com.fptuni.vms.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fptuni.vms.model.Role;
import com.fptuni.vms.model.User;
import com.fptuni.vms.model.User.UserStatus;
import com.fptuni.vms.repository.RoleRepository;
import com.fptuni.vms.repository.UserRepository;

import java.time.LocalDateTime;

@Component
public class DataSeeder {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;

        public DataSeeder(UserRepository userRepository,
                        RoleRepository roleRepository,
                        PasswordEncoder passwordEncoder) {
                this.userRepository = userRepository;
                this.roleRepository = roleRepository;
                this.passwordEncoder = passwordEncoder;
        }

        // @EventListener
        // public void seed(ApplicationReadyEvent event) {
        // seedUsers();
        // }

        private void seedUsers() {
                // // Check if users already exist
                // if (userRepository.count() > 0) {
                // System.out.println("Users already exist, skipping user seeding...");
                // return;
                // }

                System.out.println("Seeding users...");

                // Get roles
                Role adminRole = roleRepository.findByRoleName("ADMIN")
                                .orElseThrow(() -> new RuntimeException("Administrator role not found"));
                Role volunteerRole = roleRepository.findByRoleName("VOLUNTEER")
                                .orElseThrow(() -> new RuntimeException("Volunteer role not found"));
                Role orgStaffRole = roleRepository.findByRoleName("ORG_OWNER")
                                .orElseThrow(() -> new RuntimeException("Organization Staff role not found"));

                // // Create Admin user
                // User admin = new User();
                // admin.setFullName("Admin User");
                // admin.setEmail("admin@vms.com");
                // admin.setPhone("0123456789");
                // admin.setAddress("Hà Nội");
                // admin.setPasswordHash(passwordEncoder.encode("admin123"));
                // admin.setRole(adminRole);
                // admin.setStatus(UserStatus.ACTIVE);
                // admin.setCreatedAt(LocalDateTime.now());
                // admin.setAvatarUrl(
                // "https://images.pexels.com/photos/2379004/pexels-photo-2379004.jpeg?auto=compress&cs=tinysrgb&w=150");
                // userRepository.save(admin);

                // Create Volunteer user
                User volunteer = new User();
                volunteer.setFullName("Phạm Minh Tuấn");
                volunteer.setEmail("volunteer@vms.com");
                volunteer.setPhone("0901234567");
                volunteer.setAddress("Đà Nẵng");
                volunteer.setPasswordHash(passwordEncoder.encode("volunteer123"));
                volunteer.setRole(volunteerRole);
                volunteer.setStatus(UserStatus.ACTIVE);
                volunteer.setCreatedAt(LocalDateTime.now());
                volunteer.setAvatarUrl(
                                "https://images.pexels.com/photos/2379004/pexels-photo-2379004.jpeg?auto=compress&cs=tinysrgb&w=150");
                userRepository.save(volunteer);

                // // Create Organization Staff user
                // User orgStaff = new User();
                // orgStaff.setFullName("Nguyễn Văn Staff");
                // orgStaff.setEmail("staff@vms.com");
                // orgStaff.setPhone("0987654321");
                // orgStaff.setAddress("TP.HCM");
                // orgStaff.setPasswordHash(passwordEncoder.encode("staff123"));
                // orgStaff.setRole(orgStaffRole);
                // orgStaff.setStatus(UserStatus.ACTIVE);
                // orgStaff.setCreatedAt(LocalDateTime.now());
                // orgStaff.setAvatarUrl(
                // "https://images.pexels.com/photos/2379004/pexels-photo-2379004.jpeg?auto=compress&cs=tinysrgb&w=150");
                // userRepository.save(orgStaff);

                // // Create a locked user for testing
                // User lockedUser = new User();
                // lockedUser.setFullName("Locked User");
                // lockedUser.setEmail("locked@vms.com");
                // lockedUser.setPhone("0111111111");
                // lockedUser.setAddress("Locked City");
                // lockedUser.setPasswordHash(passwordEncoder.encode("locked123"));
                // lockedUser.setRole(volunteerRole);
                // lockedUser.setStatus(UserStatus.LOCKED);
                // lockedUser.setCreatedAt(LocalDateTime.now());
                // userRepository.save(lockedUser);

                System.out.println("Users seeded successfully!");
                System.out.println("=== TEST ACCOUNTS ===");
                // System.out.println("Admin: admin@vms.com / admin123");
                System.out.println("Volunteer: volunteer@vms.com / volunteer123");
                // System.out.println("Staff: staff@vms.com / staff123");
                // System.out.println("Locked: locked@vms.com / locked123 (should fail)");
                System.out.println("====================");
        }
}