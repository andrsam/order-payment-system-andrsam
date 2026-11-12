package dev.sorokin.service;

import dev.sorokin.api.dto.OrderCreateRequestDto;
import dev.sorokin.api.dto.OrderResponseDto;
import dev.sorokin.domain.Order;
import dev.sorokin.domain.Task;
import dev.sorokin.enums.PaymentStatus;
import dev.sorokin.enums.TaskStatus;
import dev.sorokin.mapper.OrderMapper;
import dev.sorokin.repository.OrderRepository;
import dev.sorokin.repository.TaskRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TaskRepository taskRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public Order createOrder(
            OrderCreateRequestDto request
    ) {
        val orderEntity = orderMapper.toOrder(request);
        val order = orderRepository.save(orderEntity);
        val taskEntity = Task.builder().orderId(order.getId()).status(TaskStatus.NEW).attempts(0L).build();
        taskRepository.save(taskEntity);
        return order;
    }

    public Optional<Order> findOrder(UUID id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public void setOrderStatus(Order order, PaymentStatus paymentStatus, BigDecimal amount) {
        switch (paymentStatus) {
            case AUTHORIZATION:
                order.setAuthorizedAmount(amount);
                break;

            case REPRICE:
                order.setFinalAmount(amount);
                break;

            case SUCCESS_PAID:
                order.setCapturedAmount(amount);
                break;
        }
        order.setPaymentStatus(paymentStatus);
        orderRepository.save(order);
    }
}
