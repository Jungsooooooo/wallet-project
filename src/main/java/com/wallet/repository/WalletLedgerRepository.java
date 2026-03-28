package com.wallet.repository;

import com.wallet.domain.WalletLedgerEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletLedgerRepository extends JpaRepository<WalletLedgerEntry, Long> {

    List<WalletLedgerEntry> findByWallet_Id(String walletId);

    List<WalletLedgerEntry> findByWallet_IdOrderByCreatedAtDesc(String walletId);
}
