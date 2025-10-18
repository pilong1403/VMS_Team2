package com.fptuni.vms.repository;

import com.fptuni.vms.model.UserAuthProvider;

import java.util.Optional;

public interface UserAuthProviderRepository {

    Optional<UserAuthProvider> findByProviderAndExternalUid(String provider, String externalUid);
}
