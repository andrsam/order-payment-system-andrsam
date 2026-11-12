package dev.sorokin.service;

import dev.sorokin.api.payment.AuthorizationStatus;
import dev.sorokin.api.payment.AuthorizePaymentRequestDto;
import dev.sorokin.api.payment.CapturePaymentRequestDto;
import dev.sorokin.api.payment.CaptureStatus;
import dev.sorokin.api.warehouse.CalculatePricingRequestDto;
import dev.sorokin.config.StubHttpClient;
import dev.sorokin.domain.Order;
import dev.sorokin.domain.Task;
import dev.sorokin.enums.PaymentStatus;
import dev.sorokin.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessor {
    private final OrderService orderService;
    private final StubHttpClient stubHttpClient;

    private record Tuple(BigDecimal amount, TaskStatus status) {
    }

    public TaskStatus process(Task task) {
        val taskId = task.getId();
        log.info("Start processing task id = [{}]", taskId);
        val orderId = task.getOrderId();

        Optional<Order> orderOptional = orderService.findOrder(orderId);
        if (orderOptional.isEmpty()) {
            log.error("Task id = [{}]: Order id = [{}] not found", orderId, taskId);
            return TaskStatus.FAILED_NON_RETRYABLE;
        }

        Order order = orderOptional.get();

        // 1. AUTH
        val authorizationResult = getAuthorizedAmount(task, order);
        if (authorizationResult.status() == TaskStatus.FAILED_NON_RETRYABLE) {
            return TaskStatus.FAILED_NON_RETRYABLE;
        }

        //2. REPRICE
        val finalAmount = getFinalAmount(taskId, order);
        if (finalAmount.compareTo(authorizationResult.amount()) > 0) {
            log.error("Task id = [{}]: Price changing failed for order id = [{}] final amount > authorizationAmount ({} > {})", taskId, orderId, finalAmount, authorizationResult.amount());
            orderService.setOrderStatus(order, PaymentStatus.PRICE_CHANGED_FAILED, authorizationResult.amount());
            return TaskStatus.FAILED_NON_RETRYABLE;
        }

        //3. CAPTURE
        return capturePayment(task, order, finalAmount);
    }

    private Tuple getAuthorizedAmount(Task task, Order order) {
        val authorizePaymentRequest = new AuthorizePaymentRequestDto(order.getCustomerId(), order.getClientEstimate());
        log.info("Task id = [{}]: starting authorize payment, authorizePaymentRequest: [{}]", task.getId(), authorizePaymentRequest);
        val response = stubHttpClient.authorizePayment(authorizePaymentRequest);
        log.info("Task id = [{}]: successfully received authorized payment. response: [{}]", task.getId(), response);

        val orderId = task.getOrderId();

        PaymentStatus paymentStatus = PaymentStatus.AUTHORIZATION;
        if (response.status() == AuthorizationStatus.DECLINED) {
            log.error("Task id = [{}]: declined order id = [{}] with reason: [{}]", task.getId(), orderId, response.message());
            paymentStatus = PaymentStatus.AUTHORIZATION_FAILED;
            task.setStatus(TaskStatus.FAILED_NON_RETRYABLE);
        } else {
            log.info("Task id = [{}]: authorized order id = [{}]", task.getId(), orderId);
        }

        orderService.setOrderStatus(order, paymentStatus, response.authorizedAmount());
        return new Tuple(response.authorizedAmount(), task.getStatus());
    }

    private BigDecimal getFinalAmount(Long taskId, Order order) {
        val orderId = order.getId();
        log.info("Task id = [{}]: start getting final amount for order with id = [{}]", taskId, orderId);
        val response = stubHttpClient.calculatePricing(new CalculatePricingRequestDto(orderId));
        val finalAmount = response.finalAmount();
        log.info("Task id = [{}]: calculated successfully final amount = [{}] for orderId = [{}]", taskId, orderId, finalAmount);
        orderService.setOrderStatus(order, PaymentStatus.REPRICE, finalAmount);
        return finalAmount;
    }

    private TaskStatus capturePayment(Task task, Order order, BigDecimal finalAmount) {
        log.info("Task id = [{}]: start capturing payment", task.getId());
        val response = stubHttpClient.capturePayment(new CapturePaymentRequestDto(finalAmount, order.getCustomerId()));

        if (response.status() == CaptureStatus.FAILED) {
            log.error("Task id = [{}]: Failed to capture order with id = [{}] with reason: [{}]", task.getId(), task.getOrderId(), response.message());
            return TaskStatus.FAILED_NON_RETRYABLE;
        }
        log.info("Task id = [{}]: successfully captured order with id = [{}]", task.getId(), task.getOrderId());
        orderService.setOrderStatus(order, PaymentStatus.SUCCESS_PAID, response.capturedAmount());
        return TaskStatus.SUCCEEDED;
    }

}
