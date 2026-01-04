package neyan.tech.baraka_backend.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import neyan.tech.baraka_backend.common.security.CurrentUser;
import neyan.tech.baraka_backend.common.security.UserPrincipal;
import neyan.tech.baraka_backend.payment.dto.PaymentResponse;
import neyan.tech.baraka_backend.payment.entity.PaymentProvider;
import neyan.tech.baraka_backend.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders/{orderId}/payment")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payments", description = "Payment endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Get payment for order", description = "Returns payment details for an order")
    @GetMapping
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @PathVariable UUID orderId,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId, currentUser.getId()));
    }

    @Operation(summary = "Update payment provider", description = "Changes payment method before payment")
    @PutMapping("/provider")
    public ResponseEntity<PaymentResponse> updatePaymentProvider(
            @PathVariable UUID orderId,
            @RequestParam PaymentProvider provider,
            @CurrentUser UserPrincipal currentUser) {
        PaymentResponse payment = paymentService.getPaymentByOrderId(orderId, currentUser.getId());
        return ResponseEntity.ok(paymentService.updatePaymentProvider(payment.getId(), provider, currentUser.getId()));
    }

    @Operation(summary = "Mark payment as paid", description = "Marks payment as paid (merchant/admin for cash)")
    @PreAuthorize("hasAnyRole('MERCHANT', 'ADMIN')")
    @PostMapping("/mark-paid")
    public ResponseEntity<PaymentResponse> markAsPaid(
            @PathVariable UUID orderId,
            @CurrentUser UserPrincipal currentUser) {
        PaymentResponse payment = paymentService.getPaymentByOrderId(orderId, currentUser.getId());
        return ResponseEntity.ok(paymentService.markAsPaid(payment.getId(), currentUser.getId()));
    }
}

