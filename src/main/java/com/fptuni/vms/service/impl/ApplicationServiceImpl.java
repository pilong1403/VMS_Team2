
package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.Application;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.ApplicationRepository;
import com.fptuni.vms.repository.OpportunityRepository;
import com.fptuni.vms.repository.UserRepository;
import com.fptuni.vms.service.ApplicationService;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ApplicationServiceImpl implements ApplicationService {

        private final ApplicationRepository applicationRepository;
        private final OpportunityRepository opportunityRepository;
        private final UserRepository userRepository;

        public ApplicationServiceImpl(ApplicationRepository applicationRepository,
                        OpportunityRepository opportunityRepository,
                        UserRepository userRepository) {
                this.applicationRepository = applicationRepository;
                this.opportunityRepository = opportunityRepository;
                this.userRepository = userRepository;
        }

        @Transactional
        @Override
        public Application applyOpportunity(Integer opportunityId, String email, String reason) {
                System.out.println("🔵 [ApplicationService] Bắt đầu xử lý nộp đơn cho opportunityId=" + opportunityId
                                + ", email=" + email);

                Opportunity opportunity = opportunityRepository.findById(opportunityId)
                                .orElseThrow(() -> new RuntimeException("Cơ hội không tồn tại."));
                System.out.println(
                                " Tìm thấy cơ hội: " + opportunity.getTitle() + ", status=" + opportunity.getStatus());
                if (opportunity.getStatus() != Opportunity.OpportunityStatus.OPEN) {
                        System.err.println(
                                        " Cơ hội không ở trạng thái OPEN, status hiện tại: " + opportunity.getStatus());
                        throw new RuntimeException("Cơ hội đã đóng.");
                }

                User volunteer = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
                System.out.println("Tìm thấy volunteer: userId=" + volunteer.getUserId() + ", email="
                                + volunteer.getEmail());

                if (applicationRepository.existsByOpportunity_OppIdAndVolunteer_UserId(opportunityId,
                                volunteer.getUserId())) {
                        System.err.println(" Volunteer đã ứng tuyển cơ hội này rồi!");
                        throw new RuntimeException("Bạn đã ứng tuyển cơ hội này rồi.");
                }
                System.out.println("Kiểm tra trùng lặp OK - tạo đơn mới");

                Application app = new Application();
                app.setOpportunity(opportunity);
                app.setVolunteer(volunteer);
                app.setReason(reason);
                app.setStatus(Application.ApplicationStatus.PENDING);
                app.setAppliedAt(LocalDateTime.now());

                System.out.println(" Đang lưu Application vào DB...");
                Application savedApp = applicationRepository.save(app);
                System.out.println(" Lưu thành công! appId=" + savedApp.getAppId() + ", volunteerId="
                                + savedApp.getVolunteer().getUserId());

                return savedApp;
        }

        // Lấy danh sách đơn theo volunteer

        @Override
        public List<Application> getApplicationsByVolunteerId(Integer userId, String q, String status, String sort) {
                System.out.println(" [ApplicationService] Lấy danh sách đơn cho userId=" + userId);
                List<Application> apps = applicationRepository.findByVolunteer_UserId(userId);
                System.out.println(" Tìm thấy " + apps.size() + " đơn từ DB");

                // --- Lọc theo tên cơ hội ---
                if (q != null && !q.trim().isEmpty()) {
                        apps = apps.stream()
                                        .filter(a -> a.getOpportunity() != null &&
                                                        a.getOpportunity().getTitle().toLowerCase()
                                                                        .contains(q.toLowerCase()))
                                        .toList();
                }

                // --- Lọc theo trạng thái ---
                if (status != null && !status.isEmpty()) {
                        apps = apps.stream()
                                        .filter(a -> a.getStatus() != null &&
                                                        a.getStatus().name().equalsIgnoreCase(status))
                                        .toList();
                }

                // --- Sắp xếp ---
                if ("oldest".equalsIgnoreCase(sort)) {
                        apps = apps.stream()
                                        .sorted(Comparator.comparing(Application::getAppliedAt))
                                        .toList();
                } else {
                        apps = apps.stream()
                                        .sorted((a, b) -> b.getAppliedAt().compareTo(a.getAppliedAt()))
                                        .toList();
                }

                return apps;
        }

        @Override
        public Integer getVolunteerIdByEmail(String email) {
                return userRepository.findByEmail(email)
                                .map(User::getUserId)
                                .orElse(null);
        }

        @Override
        @Transactional
        public Application getApplicationDetail(Integer appId) {
                return applicationRepository.findDetail(appId).orElse(null);
        }

        @Override
        @Transactional
        public void cancelApplication(Integer appId, String reason) {
                Application app = applicationRepository.findById(appId)
                                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn ứng tuyển"));

                // Nếu đã CANCELLED thì thôi
                if (app.getStatus() == Application.ApplicationStatus.CANCELLED)
                        return;

                app.setStatus(Application.ApplicationStatus.CANCELLED);
                app.setCancelReason(reason != null ? reason : "Huỷ bởi tình nguyện viên");
                app.setUpdatedAt(java.time.LocalDateTime.now());
                applicationRepository.save(app);
        }
}
