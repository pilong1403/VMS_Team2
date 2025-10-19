package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.Application;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.ApplicationRepository;
import com.fptuni.vms.service.ApplicationService;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

        private final ApplicationRepository repo;

        public ApplicationServiceImpl(ApplicationRepository repo) {
                this.repo = repo;
        }

        @Override
        public Application apply(Integer oppId, Integer volunteerId, String reason) {
                Opportunity opp = repo.findOpportunityById(oppId);
                if (opp == null)
                        throw new IllegalArgumentException("Opportunity not found: " + oppId);

                if (opp.getStatus() != Opportunity.OpportunityStatus.OPEN)
                        throw new IllegalStateException("Opportunity is not OPEN");

                if (opp.getEndTime() != null && !opp.getEndTime().isAfter(LocalDateTime.now()))
                        throw new IllegalStateException("Opportunity already ended");

                User volunteer = repo.findUserById(volunteerId);
                if (volunteer == null)
                        throw new IllegalArgumentException("Volunteer not found: " + volunteerId);

                if (repo.existsByOppIdAndVolunteerId(oppId, volunteerId))
                        throw new IllegalStateException("You already applied to this opportunity");

                Application app = new Application();
                app.setOpportunity(opp);
                app.setVolunteer(volunteer);
                app.setAppliedAt(LocalDateTime.now());
                app.setReason(reason);

                try {
                        return repo.save(app);
                } catch (PersistenceException ex) {
                        throw new IllegalStateException("You already applied to this opportunity");
                }
        }

        // @Override
        // public Application apply(Integer oppId, Integer volunteerId, String reason,
        // String fullName, String phone, String address) {

        // Opportunity opp = repo.findOpportunityById(oppId);
        // if (opp == null)
        // throw new IllegalArgumentException("Opportunity not found: " + oppId);
        // if (opp.getStatus() != Opportunity.OpportunityStatus.OPEN)
        // throw new IllegalStateException("Opportunity is not OPEN");
        // if (opp.getEndTime() != null &&
        // !opp.getEndTime().isAfter(LocalDateTime.now()))
        // throw new IllegalStateException("Opportunity already ended");

        // User volunteer = repo.findUserById(volunteerId);
        // if (volunteer == null)
        // throw new IllegalArgumentException("Volunteer not found: " + volunteerId);

        // // Cập nhật nhanh thông tin liên hệ nếu người dùng có sửa trên form
        // boolean changed = false;
        // if (fullName != null && !fullName.isBlank() &&
        // !fullName.equals(volunteer.getFullName())) {
        // volunteer.setFullName(fullName.trim());
        // changed = true;
        // }
        // if (phone != null && !phone.isBlank() && !phone.equals(volunteer.getPhone()))
        // {
        // volunteer.setPhone(phone.trim());
        // changed = true;
        // }
        // if (address != null && !address.isBlank() &&
        // !address.equals(volunteer.getAddress())) {
        // volunteer.setAddress(address.trim());
        // changed = true;
        // }
        // if (changed)
        // repo.saveUser(volunteer);

        // if (repo.existsByOppIdAndVolunteerId(oppId, volunteerId))
        // throw new IllegalStateException("You already applied to this opportunity");

        // Application app = new Application();
        // app.setOpportunity(opp);
        // app.setVolunteer(volunteer);
        // app.setAppliedAt(LocalDateTime.now());
        // app.setReason(reason);

        // try {
        // return repo.save(app);
        // } catch (PersistenceException ex) {
        // throw new IllegalStateException("You already applied to this opportunity");
        // }
        // }

        @Override
        public Application apply(Integer oppId, Integer volunteerId, String reason,
                        String fullName, String phone, String address) {
                // cập nhật thông tin user nếu FE gửi lên từ popup (không ép buộc)
                var user = repo.findUserById(volunteerId);
                if (user == null)
                        throw new IllegalArgumentException("Volunteer not found: " + volunteerId);
                boolean dirty = false;
                if (fullName != null && !fullName.isBlank() && !fullName.equals(user.getFullName())) {
                        user.setFullName(fullName);
                        dirty = true;
                }
                if (phone != null && !phone.isBlank() && !phone.equals(user.getPhone())) {
                        user.setPhone(phone);
                        dirty = true;
                }
                if (address != null && !address.isBlank() && !address.equals(user.getAddress())) {
                        user.setAddress(address);
                        dirty = true;
                }
                if (dirty)
                        repo.saveUser(user);

                // gọi lại apply gốc để tái dùng toàn bộ rule/trigger kiểm tra
                return apply(oppId, volunteerId, reason);
        }

        @Override
        public java.util.List<Application> listMyApplications(Integer volunteerId) {
                return repo.findAllByVolunteerId(volunteerId);
        }

}
