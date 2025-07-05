package com.winnguyen1905.shipping.core.feign.client;

import com.winnguyen1905.shipping.core.feign.dto.PaymentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class PaymentServiceClientFallback implements PaymentServiceClient {

    @Override
    public ResponseEntity<PaymentDto> getPaymentById(String paymentId) {
        log.warn("Payment service is unavailable, returning fallback response for payment ID: {}", paymentId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<List<PaymentDto>> getPaymentsByOrderId(Long orderId) {
        log.warn("Payment service is unavailable, returning empty payment list for order ID: {}", orderId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Collections.emptyList());
    }

    @Override
    public ResponseEntity<PaymentDto.PaymentResponse> processPayment(PaymentDto.PaymentRequest request) {
        log.warn("Payment service is unavailable, cannot process payment for order: {}", request.getOrderId());
        
        PaymentDto.PaymentResponse response = PaymentDto.PaymentResponse.builder()
            .paymentStatus("FAILED")
            .errorCode("SERVICE_UNAVAILABLE")
            .errorMessage("Payment service is temporarily unavailable. Please try again later.")
            .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @Override
    public ResponseEntity<PaymentDto.RefundResponse> processRefund(PaymentDto.RefundRequest request) {
        log.warn("Payment service is unavailable, cannot process refund for payment: {}", request.getPaymentId());
        
        PaymentDto.RefundResponse response = PaymentDto.RefundResponse.builder()
            .paymentId(request.getPaymentId())
            .refundStatus("FAILED")
            .errorMessage("Payment service is temporarily unavailable. Refund will be processed when service is restored.")
            .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @Override
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(String paymentId) {
        log.warn("Payment service is unavailable, returning fallback payment status for: {}", paymentId);
        
        PaymentStatusResponse response = new PaymentStatusResponse(
            paymentId,
            "UNKNOWN",
            null,
            BigDecimal.ZERO,
            "USD",
            Instant.now(),
            "Payment service is temporarily unavailable"
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @Override
    public ResponseEntity<PaymentDto.PaymentResponse> cancelPayment(String paymentId, CancelPaymentRequest request) {
        log.warn("Payment service is unavailable, cannot cancel payment: {}", paymentId);
        
        PaymentDto.PaymentResponse response = PaymentDto.PaymentResponse.builder()
            .paymentId(paymentId)
            .paymentStatus("CANCEL_FAILED")
            .errorCode("SERVICE_UNAVAILABLE")
            .errorMessage("Payment service is temporarily unavailable. Cannot cancel payment at this time.")
            .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @Override
    public ResponseEntity<PaymentDto.PaymentResponse> capturePayment(String paymentId, CapturePaymentRequest request) {
        log.warn("Payment service is unavailable, cannot capture payment: {}", paymentId);
        
        PaymentDto.PaymentResponse response = PaymentDto.PaymentResponse.builder()
            .paymentId(paymentId)
            .paymentStatus("CAPTURE_FAILED")
            .errorCode("SERVICE_UNAVAILABLE")
            .errorMessage("Payment service is temporarily unavailable. Cannot capture payment at this time.")
            .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @Override
    public ResponseEntity<PaymentDto.RefundResponse> getRefundById(String refundId) {
        log.warn("Payment service is unavailable, returning fallback response for refund ID: {}", refundId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<List<PaymentDto.RefundResponse>> getRefundsByPaymentId(String paymentId) {
        log.warn("Payment service is unavailable, returning empty refund list for payment ID: {}", paymentId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Collections.emptyList());
    }

    @Override
    public ResponseEntity<PaymentValidationResult> validateOrderPayment(Long orderId) {
        log.warn("Payment service is unavailable, assuming payment is valid for order: {}", orderId);
        
        // In fallback mode, we assume payment is valid to allow order processing to continue
        PaymentValidationResult result = new PaymentValidationResult(
            orderId,
            true, // Assume valid to not block shipping
            true, // Assume paid
            false, // No refunds
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of("Payment service unavailable - validation skipped")
        );
        
        return ResponseEntity.ok(result);
    }
} 
