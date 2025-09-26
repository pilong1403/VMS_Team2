package com.fptuni.vms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import com.fptuni.vms.model.VolunteerRating;

public interface VolunteerRatingRepository extends JpaRepository<VolunteerRating, Integer> {

    @Procedure(procedureName = "sp_org_rate_volunteer")
    void upsertByOrg(
            @Param("opp_id") Integer oppId,
            @Param("rater_org_id") Integer raterOrgId,
            @Param("ratee_user_id") Integer rateeUserId,
            @Param("stars") Short stars,
            @Param("comment") String comment
    );
}
