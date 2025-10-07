package com.fptuni.vms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import com.fptuni.vms.model.VolunteerRating;

public interface VolunteerRatingRepository extends JpaRepository<VolunteerRating, Integer> {


}
