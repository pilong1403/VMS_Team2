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
}
