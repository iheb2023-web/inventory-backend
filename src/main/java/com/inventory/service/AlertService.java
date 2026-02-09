package com.inventory.service;

import com.inventory.dao.AlertDao;
import com.inventory.dto.AlertWithShelfDto;
import com.inventory.model.device.Alert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertService {

    @Autowired
    private AlertDao alertDao;

    public List<Alert> getAllAlerts() {
        return alertDao.findAll();
    }

    public List<Alert> getOpenAlerts() {
        return alertDao.findOpenAlerts();
    }

    public List<AlertWithShelfDto> getAllAlertsWithShelf() {
        return alertDao.findAllWithShelf();
    }

    public List<AlertWithShelfDto> getOpenAlertsWithShelf() {
        return alertDao.findOpenAlertsWithShelf();
    }

    public long countOpenAlerts() {
        return alertDao.countOpenAlerts();
    }

    public void resolveAlert(Long alertId) {
        alertDao.resolveAlert(alertId);
    }

    public void deleteAlert(Long id) {
        alertDao.delete(id);
    }
}
