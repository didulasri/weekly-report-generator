package com.weeklyreportgenerator.backend.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreportgenerator.backend.entity.Invitation;
import com.weeklyreportgenerator.backend.entity.enums.InvitationStatus;

import jakarta.persistence.LockModeType;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    // Locks the row for the duration of the accepting transaction so a second concurrent accept
    // of the same token blocks until the first commits, then sees status = ACCEPTED and is
    // rejected -- this is what makes "two concurrent accepts create exactly one user" hold.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invitation i WHERE i.tokenHash = :tokenHash")
    Optional<Invitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    boolean existsByEmailIgnoreCaseAndStatus(String email, InvitationStatus status);

    Page<Invitation> findByStatus(InvitationStatus status, Pageable pageable);

    // Applied before every list read (and on a daily schedule for datasets nobody reads) so a
    // stale PENDING row is never served back as still-pending -- "treat expires_at < now as
    // EXPIRED at read time".
    @Modifying
    @Query("UPDATE Invitation i SET i.status = com.weeklyreportgenerator.backend.entity.enums.InvitationStatus.EXPIRED "
            + "WHERE i.status = com.weeklyreportgenerator.backend.entity.enums.InvitationStatus.PENDING "
            + "AND i.expiresAt < :now")
    int expireStalePending(@Param("now") Instant now);
}
