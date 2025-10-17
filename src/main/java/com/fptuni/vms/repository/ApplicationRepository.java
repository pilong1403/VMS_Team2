package com.fptuni.vms.repository;

import com.fptuni.vms.model.Application;
import com.fptuni.vms.model.User;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {
        boolean existsByOpportunity_OppIdAndVolunteer_UserId(Integer oppId, Integer volunteerId);

        List<Application> findByVolunteer_Email(String email);

        // Lấy danh sách đơn của 1 volunteer (theo user)
        List<Application> findByVolunteerOrderByAppliedAtDesc(User volunteer);

        // (Tùy chọn) tìm kiếm theo tên cơ hội
        @Query("SELECT a FROM Application a " +
                        "WHERE a.volunteer = :volunteer " +
                        "AND (:keyword IS NULL OR LOWER(a.opportunity.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        List<Application> searchByVolunteerAndKeyword(@Param("volunteer") User volunteer,
                        @Param("keyword") String keyword);

        // Lấy danh sách đơn của 1 volunteer (theo userId)
        @EntityGraph(attributePaths = {
                        "opportunity",
                        "opportunity.organization"
        })
        List<Application> findByVolunteer_UserId(Integer userId);

        // Chi tiết đơn ứng tuyển
        @EntityGraph(attributePaths = {
                        "opportunity",
                        "opportunity.organization",
                        "volunteer"
        })
        @Query("select a from Application a where a.appId = :appId")
        Optional<Application> findDetail(@Param("appId") Integer appId);

        // Dùng riêng cho HUỶ: cần chắc chắn fetch volunteer để lấy userId redirect
        @Query("""
                            select a from Application a
                            join fetch a.volunteer v
                            where a.appId = :appId
                        """)
        Optional<Application> findByIdWithVolunteer(@Param("appId") Integer appId);
}
