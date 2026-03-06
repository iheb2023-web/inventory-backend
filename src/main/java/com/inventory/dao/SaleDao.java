package com.inventory.dao;

import com.inventory.model.product.Sale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SaleDao {
    int insert(Sale sale);
    
    List<Sale> findRecentSales(@Param("limit") int limit);
    
    List<Sale> findAll();
}
