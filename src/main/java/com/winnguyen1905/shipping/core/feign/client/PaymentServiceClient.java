package com.winnguyen1905.shipping.core.feign.client;

import com.winnguyen1905.shipping.core.feign.dto.PaymentDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
    name = "payment-service",
    url = "${microservices.payment-service.url:http://localhost:8086}",
    fallback = PaymentServiceClientFallback.class
)
@CircuitBreaker(name = "payment-service")
@Retry(name = "payment-service")
public interface PaymentServiceClient {

    /**
     * Get payment details by payment ID
     */
    @GetMapping("/api/v1/payments/{paymentId}")
    ResponseEntity<PaymentDto> getPaymentById(@PathVariable("paymentId") String paymentId);

    /**
     * Get payments by order ID
     */
    @GetMapping("/api/v1/payments/order/{orderId}")
    ResponseEntity<List<PaymentDto>> getPaymentsByOrderId(@PathVariable("orderId") Long orderId);

    /**
     * Process a payment
     */
    @PostMapping("/api/v1/payments/process")
    ResponseEntity<PaymentDto.PaymentResponse> processPayment(@RequestBody PaymentDto.PaymentRequest request);

    /**
     * Process a refund
     */
    @PostMapping("/api/v1/payments/refund")
    ResponseEntity<PaymentDto.RefundResponse> processRefund(@RequestBody PaymentDto.RefundRequest request);

    /**
     * Get payment status
     */
    @GetMapping("/api/v1/payments/{paymentId}/status")
    ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable("paymentId") String paymentId);

    /**
     * Cancel a payment
     */
    @PostMapping("/api/v1/payments/{paymentId}/cancel")
    ResponseEntity<PaymentDto.PaymentResponse> cancelPayment(
        @PathVariable("paymentId") String paymentId,
        @RequestBody CancelPaymentRequest request
    );

    /**
     * Capture an authorized payment
     */
    @PostMapping("/api/v1/payments/{paymentId}/capture")
    ResponseEntity<PaymentDto.PaymentResponse> capturePayment(
        @PathVariable("paymentId") String paymentId,
        @RequestBody CapturePaymentRequest request
    );

    /**
     * Get refund details
     */
    @GetMapping("/api/v1/payments/refunds/{refundId}")
    ResponseEntity<PaymentDto.RefundResponse> getRefundById(@PathVariable("refundId") String refundId);

    /**
     * Get refunds for a payment
     */
    @GetMapping("/api/v1/payments/{paymentId}/refunds")
    ResponseEntity<List<PaymentDto.RefundResponse>> getRefundsByPaymentId(@PathVariable("paymentId") String paymentId);

    /**
     * Check if order has valid payment
     */
    @GetMapping("/api/v1/payments/order/{orderId}/validate")
    ResponseEntity<PaymentValidationResult> validateOrderPayment(@PathVariable("orderId") Long orderId);

    // DTOs for requests/responses
    record PaymentStatusResponse(
        String paymentId,
        String status,
        String transactionId,
        java.math.BigDecimal amount,
        String currency,
        java.time.Instant lastUpdated,
        String message
    ) {}

    record CancelPaymentRequest(
        String reason,
        String description,
        Boolean notifyCustomer
    ) {}

    record CapturePaymentRequest(
        java.math.BigDecimal amount,
        String description
    ) {}

    record PaymentValidationResult(
        Long orderId,
        Boolean isValid,
        Boolean isPaid,
        Boolean hasRefunds,
        java.math.BigDecimal totalPaid,
        java.math.BigDecimal totalRefunded,
        java.math.BigDecimal remainingAmount,
        List<String> validationErrors
    ) {}
} 
