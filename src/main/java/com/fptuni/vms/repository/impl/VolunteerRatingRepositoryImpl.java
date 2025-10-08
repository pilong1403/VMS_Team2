package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.VolunteerRating;
import com.fptuni.vms.repository.VolunteerRatingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public class VolunteerRatingRepositoryImpl implements VolunteerRatingRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void save(VolunteerRating rating) {
        if (rating.getId() == null) {
            em.persist(rating);
        } else {
            em.merge(rating);
        }
    }

    @Override
    public long countAll() {
        String jpql = "SELECT COUNT(vr) FROM VolunteerRating vr";
        return em.createQuery(jpql, Long.class).getSingleResult();
    }

    @Override
    public long countByStars(short stars) {
        String jpql = "SELECT COUNT(vr) FROM VolunteerRating vr WHERE vr.stars = :stars";
        return em.createQuery(jpql, Long.class)
                .setParameter("stars", stars)
                .getSingleResult();
    }

    @Override
    public List<VolunteerRating> findByStars(short stars) {
        String jpql = "SELECT vr FROM VolunteerRating vr WHERE vr.stars = :stars ORDER BY vr.createdAt DESC";
        return em.createQuery(jpql, VolunteerRating.class)
                .setParameter("stars", stars)
                .getResultList();
    }
}
