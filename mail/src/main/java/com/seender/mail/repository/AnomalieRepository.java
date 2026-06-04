package com.seender.mail.repository;

import com.seender.mail.entity.Anomalie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnomalieRepository extends JpaRepository<Anomalie, Long> {

    // Hadi methode ghan-7tajjouha bach n-9ellbou 3la l'description b l'code
    Optional<Anomalie> findByCode(String code);
}