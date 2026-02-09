package com.inventory.controller;

import com.inventory.dto.AlertWithShelfDto;
import com.inventory.model.device.Alert;
import com.inventory.service.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "http://localhost:4200")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @GetMapping
    public ResponseEntity<List<AlertWithShelfDto>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlertsWithShelf());
    }

    @GetMapping("/open")
    public ResponseEntity<List<AlertWithShelfDto>> getOpenAlerts() {
        return ResponseEntity.ok(alertService.getOpenAlertsWithShelf());
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getOpenAlertsCount() {
        return ResponseEntity.ok(alertService.countOpenAlerts());
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<Void> resolveAlert(@PathVariable Long id) {
        alertService.resolveAlert(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        alertService.deleteAlert(id);
        return ResponseEntity.ok().build();
    }
}
