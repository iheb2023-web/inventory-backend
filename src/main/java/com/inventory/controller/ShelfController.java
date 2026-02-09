package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.ShelfRequest;
import com.inventory.dto.ShelfWeightRequest;
import com.inventory.model.store.Shelf;
import com.inventory.service.ShelfService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shelf")
@RequiredArgsConstructor
public class ShelfController {

    private final ShelfService shelfService;

    @GetMapping
    public ApiResponse<List<Shelf>> getAll() {
        List<Shelf> shelves = shelfService.getAllShelves();
        return ApiResponse.ok("SHELVES_LIST", shelves);
    }

    @GetMapping("/{id}")
    public ApiResponse<Shelf> getById(@PathVariable Long id) {
        Shelf shelf = shelfService.getShelfById(id);
        return ApiResponse.ok("SHELF_FOUND", shelf);
    }

    @PostMapping
    public ApiResponse<Shelf> create(@RequestBody ShelfRequest req) {
        Shelf shelf = shelfService.createShelf(
            req.getName(),
            req.getMaxWeight(),
            req.getMinThreshold()
        );
        return ApiResponse.ok("SHELF_CREATED", shelf);
    }

    @PutMapping("/{id}")
    public ApiResponse<Shelf> update(@PathVariable Long id, @RequestBody ShelfRequest req) {
        Shelf shelf = shelfService.updateShelf(
            id,
            req.getName(),
            req.getMaxWeight(),
            req.getMinThreshold()
        );
        return ApiResponse.ok("SHELF_UPDATED", shelf);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        shelfService.deleteShelf(id);
        return ApiResponse.ok("SHELF_DELETED", null);
    }

    @PostMapping("/weight")
    public ApiResponse<Void> updateWeight(@RequestBody ShelfWeightRequest req) {

        shelfService.updateShelfWeight(req.getShelfId(), req.getCurrentWeight());
        return ApiResponse.ok("WEIGHT_UPDATED", null);
    }
}
