package com.inventory.service;

import com.inventory.dao.AlertDao;
import com.inventory.dao.ShelfDao;
import com.inventory.dto.AlertWithShelfDto;
import com.inventory.model.device.Alert;
import com.inventory.model.store.Shelf;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelfService {

    private final ShelfDao shelfDao;
    private final AlertDao alertDao;
    private final SimpMessagingTemplate messagingTemplate;

    public List<Shelf> getAllShelves() {
        return shelfDao.findAll();
    }

    public Shelf getShelfById(Long id) {
        return shelfDao.findById(id);
    }

    @Transactional
    public Shelf createShelf(String name, BigDecimal maxWeight, BigDecimal minThreshold) {
        Shelf shelf = new Shelf();
        shelf.setName(name);
        shelf.setMaxWeight(maxWeight);
        shelf.setMinThreshold(minThreshold);
        shelf.setCurrentWeight(BigDecimal.ZERO);
        shelfDao.insert(shelf);
        return shelf;
    }

    @Transactional
    public Shelf updateShelf(Long id, String name, BigDecimal maxWeight, BigDecimal minThreshold) {
        Shelf shelf = shelfDao.findById(id);
        if (shelf == null) throw new RuntimeException("Shelf not found");
        
        shelf.setName(name);
        shelf.setMaxWeight(maxWeight);
        shelf.setMinThreshold(minThreshold);
        shelfDao.update(shelf);
        return shelf;
    }

    @Transactional
    public void deleteShelf(Long id) {
        shelfDao.delete(id);
    }

    @Transactional
    public void updateShelfWeight(Long shelfId, double weight) {

        Shelf shelf = shelfDao.findById(shelfId);
        if (shelf == null) throw new RuntimeException("Shelf not found");

        shelfDao.updateCurrentWeight(shelfId, weight);

        if (weight < shelf.getMinThreshold().doubleValue()) {
            Alert open = alertDao.findOpenAlertByShelf(shelfId);
            if (open == null) {
                Alert alert = new Alert();
                alert.setShelfId(shelfId);
                alert.setAlertType(Alert.AlertType.LOW_WEIGHT);
                alert.setStatus(Alert.AlertStatus.OPEN);
                alertDao.insert(alert);
                
                // Send alert via WebSocket
                List<AlertWithShelfDto> alerts = alertDao.findOpenAlertsWithShelf();
                AlertWithShelfDto newAlert = alerts.stream()
                    .filter(a -> a.getId().equals(alert.getId()))
                    .findFirst()
                    .orElse(null);
                
                if (newAlert != null) {
                    messagingTemplate.convertAndSend("/topic/alerts", newAlert);
                }
            }
        } else {
            Alert open = alertDao.findOpenAlertByShelf(shelfId);
            if (open != null) {
                alertDao.resolveAlert(open.getId());
            }
        }
    }
}
