package neyan.tech.ni3ma_backend.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neyan.tech.ni3ma_backend.common.exception.BadRequestException;
import neyan.tech.ni3ma_backend.common.exception.ForbiddenException;
import neyan.tech.ni3ma_backend.common.exception.NotFoundException;
import neyan.tech.ni3ma_backend.notification.entity.NotificationType;
import neyan.tech.ni3ma_backend.notification.service.NotificationService;
import neyan.tech.ni3ma_backend.order.entity.Order;
import neyan.tech.ni3ma_backend.order.repository.OrderRepository;
import neyan.tech.ni3ma_backend.payment.dto.PaymentResponse;
import neyan.tech.ni3ma_backend.payment.dto.UpdatePaymentStatusRequest;
import neyan.tech.ni3ma_backend.payment.entity.Payment;
import neyan.tech.ni3ma_backend.payment.entity.PaymentProvider;
import neyan.tech.ni3ma_backend.payment.entity.PaymentStatus;
import neyan.tech.ni3ma_backend.payment.mapper.PaymentMapper;
import neyan.tech.ni3ma_backend.payment.repository.PaymentRepository;
import neyan.tech.ni3ma_backend.shop.service.ShopService;
import neyan.tech.ni3ma_backend.user.entity.User;
import neyan.tech.ni3ma_backend.user.entity.UserRole;
import neyan.tech.ni3ma_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ShopService shopService;
    private final PaymentMapper paymentMapper;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId, UUID userId) {
        Payment payment = findPaymentOrThrow(paymentId);
        checkPaymentAccess(payment, userId);
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(UUID orderId, UUID userId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Payment", "orderId", orderId));
        checkPaymentAccess(payment, userId);
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(UUID userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable)
                .map(paymentMapper::toResponse);
    }

    @Transactional
    public PaymentResponse updatePaymentProvider(UUID paymentId, PaymentProvider provider, UUID userId) {
        Payment payment = findPaymentOrThrow(paymentId);

        // Only customer can change payment provider before payment
        if (!payment.getOrder().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the order owner can change payment provider");
        }

        if (payment.getStatus() != PaymentStatus.UNPAID) {
            throw new BadRequestException("Cannot change provider for non-unpaid payment");
        }

        payment.setProvider(provider);
        payment = paymentRepository.save(payment);

        log.info("Payment {} provider updated to {}", paymentId, provider);
        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse markAsPending(UUID paymentId, UUID userId) {
        Payment payment = findPaymentOrThrow(paymentId);
        checkPaymentAccess(payment, userId);

        if (payment.getStatus() != PaymentStatus.UNPAID) {
            throw new BadRequestException("Can only mark unpaid payments as pending");
        }

        payment.setStatus(PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        log.info("Payment {} marked as pending", paymentId);
        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse markAsPaid(UUID paymentId, UUID merchantId) {
        Payment payment = findPaymentOrThrow(paymentId);

        // Verify merchant owns the shop
        shopService.checkShopOwnership(payment.getOrder().getBasket().getShop(), merchantId);

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Payment is already marked as paid");
        }

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BadRequestException("Cannot mark refunded payment as paid");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        payment = paymentRepository.save(payment);

        log.info("Payment {} marked as paid", paymentId);

        // Notify customer
        notificationService.createNotification(
                payment.getOrder().getUser().getId(),
                "Paiement reçu",
                String.format("Votre paiement de %s %s a été confirmé.",
                        payment.getOrder().getTotalPrice(),
                        payment.getOrder().getBasket().getCurrency()),
                NotificationType.PAYMENT_RECEIVED
        );

        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(UUID paymentId, UUID adminId) {
        Payment payment = findPaymentOrThrow(paymentId);

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("User", adminId));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Only admins can refund payments");
        }

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Can only refund paid payments");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment = paymentRepository.save(payment);

        log.info("Payment {} refunded by admin {}", paymentId, adminId);
        return paymentMapper.toResponse(payment);
    }

    // ==================== Internal Methods ====================

    public Payment findPaymentOrThrow(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment", paymentId));
    }

    private void checkPaymentAccess(Payment payment, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        Order order = payment.getOrder();
        boolean isCustomer = order.getUser().getId().equals(userId);
        boolean isMerchant = order.getBasket().getShop().getCreatedBy().getId().equals(userId);

        if (!isCustomer && !isMerchant) {
            throw new ForbiddenException("You don't have access to this payment");
        }
    }
}

