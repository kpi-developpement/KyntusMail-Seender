package com.seender.mail.repository;

import com.seender.mail.entity.MailHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MailHistoryRepository extends JpaRepository<MailHistory, Long> {
    // Njbdouhoum m-sttfin mn jdid l'9dim
    List<MailHistory> findAllByOrderByCreatedAtDesc();
}