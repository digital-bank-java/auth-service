package com.digitalbank.authservice.adapter.out.session;

import com.digitalbank.authservice.domain.model.SessionStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataSessionRepository extends JpaRepository<SessionJpaEntity, UUID> {

    @Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(:username, 0))", nativeQuery = true)
    void acquireUsernameLock(@Param("username") String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SessionJpaEntity session
               set session.status = :revokedStatus
             where session.username = :username
               and session.status = :activeStatus
               and session.createdAt <= :at
               and session.expiresAt > :at
            """)
    int revokeActiveByUsername(
            @Param("username") String username,
            @Param("at") Instant at,
            @Param("activeStatus") SessionStatus activeStatus,
            @Param("revokedStatus") SessionStatus revokedStatus);

    @Modifying
    @Query("""
            update SessionJpaEntity session
               set session.status = :revokedStatus
             where session.sessionId = :sessionId
               and session.status = :activeStatus
            """)
    int revokeIfActive(
            @Param("sessionId") UUID sessionId,
            @Param("activeStatus") SessionStatus activeStatus,
            @Param("revokedStatus") SessionStatus revokedStatus);
}
