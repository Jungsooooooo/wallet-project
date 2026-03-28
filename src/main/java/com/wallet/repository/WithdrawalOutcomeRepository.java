package com.wallet.repository;

import com.wallet.domain.WithdrawalOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawalOutcomeRepository extends JpaRepository<WithdrawalOutcome, String> {
}
