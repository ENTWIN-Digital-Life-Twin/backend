package com.digitallifetwin.auth.repository;

import com.digitallifetwin.auth.entity.RefreshToken;
import com.digitallifetwin.auth.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("""
            SELECT rt FROM RefreshToken rt
            JOIN FETCH rt.user u
            LEFT JOIN FETCH u.roles
            WHERE rt.token = :token
            """)
    Optional<RefreshToken> findByTokenWithUser(@Param("token") String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    int revokeAllActiveByUser(@Param("user") User user);
}
