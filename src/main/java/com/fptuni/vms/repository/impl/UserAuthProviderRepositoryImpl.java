package com.fptuni.vms.repository.impl;

import com.fptuni.vms.model.UserAuthProvider;
import com.fptuni.vms.repository.UserAuthProviderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserAuthProviderRepositoryImpl implements UserAuthProviderRepository {

    @PersistenceContext
    private EntityManager em; // container-managed, transaction-scoped

    @Override
    public Optional<UserAuthProvider> findByProviderAndExternalUid(String provider, String externalUid) {
        if (provider == null || provider.isBlank() || externalUid == null || externalUid.isBlank()) {
            return Optional.empty();
        }

        TypedQuery<UserAuthProvider> q = em.createQuery(
                "SELECT uap FROM UserAuthProvider uap " +
                        "WHERE uap.provider = :provider AND uap.externalUid = :externalUid",
                UserAuthProvider.class
        );
        q.setParameter("provider", provider);
        q.setParameter("externalUid", externalUid);

        List<UserAuthProvider> list = q.getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
