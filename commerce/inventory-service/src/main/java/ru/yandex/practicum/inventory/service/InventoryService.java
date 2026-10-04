package ru.yandex.practicum.inventory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.Inventory;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.mapper.InventoryMapper;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<InventoryDto> getAll() {
        return inventoryRepository.findAll().stream().map(InventoryMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public InventoryDto getByProductId(Long productId) {
        return InventoryMapper.toDto(getEntity(productId));
    }

    @Transactional
    public InventoryDto create(UpdateInventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.productId())) {
            throw new IllegalArgumentException(
                    "Складская запись для товара с productId=" + request.productId() + " уже существует");
        }
        Inventory inventory = Inventory.builder()
                .productId(request.productId())
                .quantity(request.quantity())
                .reservedQuantity(0)
                .build();
        return InventoryMapper.toDto(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryDto update(UpdateInventoryRequest request) {
        Inventory inventory = getEntity(request.productId());
        inventory.setQuantity(request.quantity());
        return InventoryMapper.toDto(inventory);
    }

    @Transactional
    public ReserveResponse reserve(ReserveRequest request) {
        Inventory inventory = getEntity(request.productId());
        int available = inventory.getQuantity() - inventory.getReservedQuantity();
        if (available < request.quantity()) {
            throw new InsufficientStockException(
                    "Недостаточно товара на складе: доступно " + available + ", запрошено " + request.quantity());
        }
        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());
        int newAvailable = inventory.getQuantity() - inventory.getReservedQuantity();
        return new ReserveResponse(true, newAvailable, "Товар успешно зарезервирован");
    }

    private Inventory getEntity(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException(
                        "Складская запись для товара с productId=" + productId + " не найдена"));
    }
}
