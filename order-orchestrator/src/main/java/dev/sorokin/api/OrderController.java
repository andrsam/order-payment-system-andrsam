package dev.sorokin.api;

import dev.sorokin.api.dto.OrderCreateRequestDto;
import dev.sorokin.api.dto.OrderResponseDto;
import dev.sorokin.mapper.OrderMapper;
import dev.sorokin.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @Operation(summary = "Формирование заказа")
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @RequestBody OrderCreateRequestDto orderCreateRequestDto
    ) {
        log.info("Received request to create order: request=[{}]", orderCreateRequestDto);
        var created = orderService.createOrder(orderCreateRequestDto);
        log.info("Created order: created={}", created);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderMapper.toOrderResponseDto(created));
    }

    @Operation(summary = "Получение сведений о заказе")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrder(
            @PathVariable UUID id
    ) {
        log.info("Getting order with id = [{}]", id);
        var foundOrder = orderService.findOrder(id);
        return foundOrder
                .map(orderMapper::toOrderResponseDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
