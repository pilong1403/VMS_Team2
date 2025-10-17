// com/fptuni/vms/repository/UserAuthProviderRepository.java
package com.fptuni.vms.repository;

import com.fptuni.vms.model.UserAuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAuthProviderRepository extends JpaRepository<UserAuthProvider, Integer> {
    Optional<UserAuthProvider> findByProviderAndExternalUid(String provider, String externalUid);
}
