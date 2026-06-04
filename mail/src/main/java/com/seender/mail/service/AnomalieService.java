package com.seender.mail.service;

import com.seender.mail.entity.Anomalie;
import com.seender.mail.repository.AnomalieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnomalieService {

    private final AnomalieRepository anomalieRepository;

    // Njabdou ga3 les anomalies bach n-affichiwhom f l'interface dyal Next.js
    public List<Anomalie> getAllAnomalies() {
        return anomalieRepository.findAll();
    }

    // Nzidou wela n-modifiw anomalie
    public Anomalie saveAnomalie(Anomalie anomalie) {
        // Hna n9dro nzidou vérification (Risk Management): wach l'code mktoub b majuscule, etc.
        anomalie.setCode(anomalie.getCode().trim().toUpperCase());
        return anomalieRepository.save(anomalie);
    }

    // Nmsse7ou anomalie ila wlat obsolète
    public void deleteAnomalie(Long id) {
        anomalieRepository.deleteById(id);
    }
}