package com.digitallifetwin.auth.repository;

import com.digitallifetwin.auth.entity.UserIdentity;
import com.digitallifetwin.auth.enums.IdentityProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

    @Query("""
            SELECT i FROM UserIdentity i
            JOIN FETCH i.user u
            LEFT JOIN FETCH u.roles
            WHERE i.provider = :provider AND i.providerUserId = :providerUserId
            """)
    Optional<UserIdentity> findByProviderAndProviderUserId(
            @Param("provider") IdentityProvider provider,
            @Param("providerUserId") String providerUserId);
}
