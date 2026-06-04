package com.seender.mail.controller;

import com.seender.mail.entity.Anomalie;
import com.seender.mail.service.AnomalieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Risk mitigation: kan-ssm7ou l Next.js y-tconnecta bla CORS errors
public class AnomalieController {

    private final AnomalieService anomalieService;

    // GET: http://localhost:4778/api/anomalies
    @GetMapping
    public ResponseEntity<List<Anomalie>> getAll() {
        return ResponseEntity.ok(anomalieService.getAllAnomalies());
    }

    // POST: http://localhost:4778/api/anomalies
    @PostMapping
    public ResponseEntity<Anomalie> createOrUpdate(@RequestBody Anomalie anomalie) {
        return ResponseEntity.ok(anomalieService.saveAnomalie(anomalie));
    }

    // DELETE: http://localhost:4778/api/anomalies/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        anomalieService.deleteAnomalie(id);
        return ResponseEntity.noContent().build();
    }
}