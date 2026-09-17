package org.example.pft.repository;

import org.example.pft.entity.TwoFactorChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TwoFactorChallengeRepository extends JpaRepository<TwoFactorChallenge, Long> {
    Optional<TwoFactorChallenge> findByChallengeId(String challengeId);
    Optional<TwoFactorChallenge> findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(
            Long userId
    );
}
