package com.inventory.dao;

import com.inventory.dto.AlertWithShelfDto;
import com.inventory.model.device.Alert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlertDao {
    Alert findOpenAlertByShelf(@Param("shelfId") Long shelfId);

    List<Alert> findAll();

    List<Alert> findOpenAlerts();

    List<AlertWithShelfDto> findAllWithShelf();

    List<AlertWithShelfDto> findOpenAlertsWithShelf();

    long countOpenAlerts();

    int insert(Alert alert);

    int resolveAlert(@Param("alertId") Long alertId);

    int delete(@Param("id") Long id);
}
