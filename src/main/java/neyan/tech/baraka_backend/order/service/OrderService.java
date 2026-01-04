package neyan.tech.baraka_backend.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neyan.tech.baraka_backend.basket.entity.Basket;
import neyan.tech.baraka_backend.basket.entity.BasketStatus;
import neyan.tech.baraka_backend.basket.service.BasketService;
import neyan.tech.baraka_backend.common.config.BarakaProperties;
import neyan.tech.baraka_backend.common.exception.BadRequestException;
import neyan.tech.baraka_backend.common.exception.ForbiddenException;
import neyan.tech.baraka_backend.common.exception.NotFoundException;
import neyan.tech.baraka_backend.notification.entity.NotificationType;
import neyan.tech.baraka_backend.notification.service.NotificationService;
import neyan.tech.baraka_backend.order.dto.CreateOrderRequest;
import neyan.tech.baraka_backend.order.dto.OrderResponse;
import neyan.tech.baraka_backend.order.dto.OrderSummaryResponse;
import neyan.tech.baraka_backend.order.entity.Order;
import neyan.tech.baraka_backend.order.entity.OrderStatus;
import neyan.tech.baraka_backend.order.mapper.OrderMapper;
import neyan.tech.baraka_backend.order.repository.OrderRepository;
import neyan.tech.baraka_backend.payment.entity.Payment;
import neyan.tech.baraka_backend.payment.entity.PaymentProvider;
import neyan.tech.baraka_backend.payment.entity.PaymentStatus;
import neyan.tech.baraka_backend.payment.repository.PaymentRepository;
import neyan.tech.baraka_backend.shop.service.ShopService;
import neyan.tech.baraka_backend.user.entity.User;
import neyan.tech.baraka_backend.user.entity.UserRole;
import neyan.tech.baraka_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final BasketService basketService;
    private final ShopService shopService;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;
    private final BarakaProperties barakaProperties;

    private static final String PICKUP_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Create a new order (reservation) - CRITICAL TRANSACTION
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrderResponse createOrder(CreateOrderRequest request, UUID customerId) {
        log.info("Creating order for customer {} on basket {}", customerId, request.getBasketId());

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("User", customerId));

        Basket basket = basketService.findBasketOrThrow(request.getBasketId());

        // Validations
        validateBasketForOrder(basket, request.getQuantity());

        // Calculate prices
        BigDecimal unitPrice = basket.getPriceDiscount();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        // Generate unique pickup code
        String pickupCode = generateUniquePickupCode();

        // Create order
        Order order = Order.builder()
                .user(customer)
                .basket(basket)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .status(OrderStatus.RESERVED)
                .pickupCode(pickupCode)
                .build();

        order = orderRepository.save(order);

        // Decrement basket quantity (may trigger sold_out)
        basketService.decrementQuantity(basket, request.getQuantity());

        // Create payment record
        Payment payment = Payment.builder()
                .order(order)
                .provider(PaymentProvider.CASH) // Default, can be changed
                .status(PaymentStatus.UNPAID)
                .build();
        paymentRepository.save(payment);

        log.info("Order created: {} with pickup code: {}", order.getId(), pickupCode);

        // Send notification
        notificationService.createNotification(
                customer.getId(),
                "Commande confirmée !",
                String.format("Votre commande chez %s est confirmée. Code: %s", 
                        basket.getShop().getName(), pickupCode),
                NotificationType.ORDER_CONFIRMED
        );

        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId, UUID userId) {
        Order order = findOrderOrThrow(orderId);
        checkOrderAccess(order, userId);
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByPickupCode(String pickupCode) {
        Order order = orderRepository.findByPickupCode(pickupCode.toUpperCase())
                .orElseThrow(() -> new NotFoundException("Order", "pickupCode", pickupCode));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(UUID customerId, Pageable pageable) {
        return orderRepository.findByUserId(customerId, pageable)
                .map(orderMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrdersByStatus(UUID customerId, OrderStatus status, Pageable pageable) {
        return orderRepository.findByUserIdAndStatus(customerId, status, pageable)
                .map(orderMapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getShopOrders(UUID shopId, UUID merchantId, Pageable pageable) {
        var shop = shopService.findShopOrThrow(shopId);
        shopService.checkShopOwnership(shop, merchantId);

        return orderRepository.findByShopId(shopId, pageable)
                .map(orderMapper::toSummaryResponse);
    }

    /**
     * Validate pickup by merchant using pickup code
     */
    @Transactional
    public OrderResponse validatePickup(String pickupCode, UUID merchantId) {
        log.info("Validating pickup with code: {}", pickupCode);

        Order order = orderRepository.findByPickupCode(pickupCode.toUpperCase())
                .orElseThrow(() -> new NotFoundException("Order", "pickupCode", pickupCode));

        // Verify merchant owns the shop
        shopService.checkShopOwnership(order.getBasket().getShop(), merchantId);

        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new BadRequestException("Order is not in RESERVED status. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.PICKED_UP);
        order = orderRepository.save(order);

        // Update payment to PAID if cash
        paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
            if (payment.getProvider() == PaymentProvider.CASH) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(Instant.now());
                paymentRepository.save(payment);
            }
        });

        log.info("Order {} picked up", order.getId());

        // Notify customer
        notificationService.createNotification(
                order.getUser().getId(),
                "Commande récupérée !",
                String.format("Votre commande chez %s a été récupérée. Bon appétit !", 
                        order.getBasket().getShop().getName()),
                NotificationType.ORDER_READY
        );

        return orderMapper.toResponse(order);
    }

    /**
     * Cancel order by customer
     */
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID customerId) {
        log.info("Cancelling order: {} by customer: {}", orderId, customerId);

        Order order = findOrderOrThrow(orderId);

        // Check ownership
        if (!order.getUser().getId().equals(customerId)) {
            throw new ForbiddenException("You can only cancel your own orders");
        }

        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new BadRequestException("Only reserved orders can be cancelled");
        }

        // Check cancellation deadline
        int cutoffMinutes = barakaProperties.getOrder().getCancelCutoffMinutes();
        Instant pickupStart = order.getBasket().getPickupStart();
        Instant cancellationDeadline = pickupStart.minus(cutoffMinutes, ChronoUnit.MINUTES);

        if (Instant.now().isAfter(cancellationDeadline)) {
            throw new BadRequestException(
                    String.format("Cannot cancel order less than %d minutes before pickup start", cutoffMinutes));
        }

        // Cancel order
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        // Restore basket quantity
        basketService.incrementQuantity(order.getBasket(), order.getQuantity());

        // Update payment
        paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PAID) {
                payment.setStatus(PaymentStatus.REFUNDED);
            }
            paymentRepository.save(payment);
        });

        log.info("Order {} cancelled", orderId);

        // Notify
        notificationService.createNotification(
                order.getUser().getId(),
                "Commande annulée",
                String.format("Votre commande chez %s a été annulée.", 
                        order.getBasket().getShop().getName()),
                NotificationType.ORDER_CANCELLED
        );

        return orderMapper.toResponse(order);
    }

    /**
     * Mark order as no-show (by merchant or admin)
     */
    @Transactional
    public OrderResponse markAsNoShow(UUID orderId, UUID merchantId) {
        Order order = findOrderOrThrow(orderId);
        shopService.checkShopOwnership(order.getBasket().getShop(), merchantId);

        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new BadRequestException("Only reserved orders can be marked as no-show");
        }

        // Check if pickup window has passed
        if (Instant.now().isBefore(order.getBasket().getPickupEnd())) {
            throw new BadRequestException("Cannot mark as no-show before pickup window ends");
        }

        order.setStatus(OrderStatus.NO_SHOW);
        order = orderRepository.save(order);

        log.info("Order {} marked as no-show", orderId);
        return orderMapper.toResponse(order);
    }

    // ==================== Internal Methods ====================

    public Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));
    }

    private void validateBasketForOrder(Basket basket, int requestedQuantity) {
        if (basket.getStatus() != BasketStatus.PUBLISHED) {
            throw new BadRequestException("Basket is not available. Status: " + basket.getStatus());
        }

        if (basket.getPickupEnd().isBefore(Instant.now())) {
            throw new BadRequestException("Basket pickup window has expired");
        }

        if (basket.getQuantityLeft() < requestedQuantity) {
            throw new BadRequestException(
                    String.format("Not enough quantity available. Requested: %d, Available: %d",
                            requestedQuantity, basket.getQuantityLeft()));
        }

        int maxQuantity = barakaProperties.getBasket().getMaxQuantityPerOrder();
        if (requestedQuantity > maxQuantity) {
            throw new BadRequestException(
                    String.format("Maximum quantity per order is %d", maxQuantity));
        }
    }

    private void checkOrderAccess(Order order, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));

        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        boolean isCustomer = order.getUser().getId().equals(userId);
        boolean isMerchant = order.getBasket().getShop().getCreatedBy().getId().equals(userId);

        if (!isCustomer && !isMerchant) {
            throw new ForbiddenException("You don't have access to this order");
        }
    }

    private String generateUniquePickupCode() {
        int length = barakaProperties.getOrder().getPickupCodeLength();
        String code;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            code = generateRandomCode(length);
            attempts++;
            if (attempts > maxAttempts) {
                throw new RuntimeException("Failed to generate unique pickup code after " + maxAttempts + " attempts");
            }
        } while (orderRepository.existsByPickupCode(code));

        return code;
    }

    private String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PICKUP_CODE_CHARS.charAt(RANDOM.nextInt(PICKUP_CODE_CHARS.length())));
        }
        return sb.toString();
    }
}

