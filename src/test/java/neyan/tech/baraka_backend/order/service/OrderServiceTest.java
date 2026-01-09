package neyan.tech.baraka_backend.order.service;

import neyan.tech.baraka_backend.basket.entity.Basket;
import neyan.tech.baraka_backend.basket.entity.BasketStatus;
import neyan.tech.baraka_backend.basket.service.BasketService;
import neyan.tech.baraka_backend.common.config.BarakaProperties;
import neyan.tech.baraka_backend.common.exception.BadRequestException;
import neyan.tech.baraka_backend.common.exception.ForbiddenException;
import neyan.tech.baraka_backend.notification.service.NotificationService;
import neyan.tech.baraka_backend.order.dto.CreateOrderRequest;
import neyan.tech.baraka_backend.order.dto.OrderResponse;
import neyan.tech.baraka_backend.order.entity.Order;
import neyan.tech.baraka_backend.order.entity.OrderStatus;
import neyan.tech.baraka_backend.order.mapper.OrderMapper;
import neyan.tech.baraka_backend.order.repository.OrderRepository;
import neyan.tech.baraka_backend.payment.entity.Payment;
import neyan.tech.baraka_backend.payment.entity.PaymentProvider;
import neyan.tech.baraka_backend.payment.entity.PaymentStatus;
import neyan.tech.baraka_backend.payment.repository.PaymentRepository;
import neyan.tech.baraka_backend.shop.entity.Shop;
import neyan.tech.baraka_backend.shop.entity.ShopStatus;
import neyan.tech.baraka_backend.shop.service.ShopService;
import neyan.tech.baraka_backend.user.entity.User;
import neyan.tech.baraka_backend.user.entity.UserRole;
import neyan.tech.baraka_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BasketService basketService;

    @Mock
    private ShopService shopService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private BarakaProperties barakaProperties;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    private User customer;
    private User merchant;
    private Shop shop;
    private Basket basket;
    private Order order;

    @BeforeEach
    void setUp() {
        // Setup mock properties
        BarakaProperties.OrderProperties orderProps = new BarakaProperties.OrderProperties();
        orderProps.setCancelCutoffMinutes(30);
        orderProps.setPickupCodeLength(6);
        
        BarakaProperties.BasketProperties basketProps = new BarakaProperties.BasketProperties();
        basketProps.setMaxQuantityPerOrder(5);

        lenient().when(barakaProperties.getOrder()).thenReturn(orderProps);
        lenient().when(barakaProperties.getBasket()).thenReturn(basketProps);

        // Setup test data
        customer = User.builder()
                .id(UUID.randomUUID())
                .fullName("Test Customer")
                .email("customer@test.com")
                .phone("+22900000001")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        merchant = User.builder()
                .id(UUID.randomUUID())
                .fullName("Test Merchant")
                .email("merchant@test.com")
                .phone("+22900000002")
                .role(UserRole.MERCHANT)
                .isActive(true)
                .build();

        shop = Shop.builder()
                .id(UUID.randomUUID())
                .name("Test Shop")
                .status(ShopStatus.ACTIVE)
                .createdBy(merchant)
                .build();

        basket = Basket.builder()
                .id(UUID.randomUUID())
                .shop(shop)
                .title("Test Basket")
                .priceOriginal(BigDecimal.valueOf(5000))
                .priceDiscount(BigDecimal.valueOf(2500))
                .currency("MRU")
                .quantityTotal(10)
                .quantityLeft(5)
                .pickupStart(Instant.now().plus(2, ChronoUnit.HOURS))
                .pickupEnd(Instant.now().plus(6, ChronoUnit.HOURS))
                .status(BasketStatus.PUBLISHED)
                .build();

        order = Order.builder()
                .id(UUID.randomUUID())
                .user(customer)
                .basket(basket)
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(2500))
                .totalPrice(BigDecimal.valueOf(2500))
                .status(OrderStatus.RESERVED)
                .pickupCode("ABC123")
                .build();
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully and decrement quantity")
        void createOrder_Success_DecrementsQuantity() {
            // Given
            CreateOrderRequest request = new CreateOrderRequest(basket.getId(), 1);

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(basketService.findBasketOrThrow(basket.getId())).thenReturn(basket);
            when(orderRepository.existsByPickupCode(any())).thenReturn(false);
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(UUID.randomUUID());
                return o;
            });
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderMapper.toResponse(any(Order.class))).thenReturn(new OrderResponse());

            // When
            OrderResponse response = orderService.createOrder(request, customer.getId());

            // Then
            assertThat(response).isNotNull();

            // Verify order was saved
            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getUser()).isEqualTo(customer);
            assertThat(savedOrder.getBasket()).isEqualTo(basket);
            assertThat(savedOrder.getQuantity()).isEqualTo(1);
            assertThat(savedOrder.getUnitPrice()).isEqualTo(BigDecimal.valueOf(2500));
            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.RESERVED);
            assertThat(savedOrder.getPickupCode()).isNotBlank();

            // Verify quantity was decremented
            verify(basketService).decrementQuantity(eq(basket), eq(1));

            // Verify payment was created
            verify(paymentRepository).save(paymentCaptor.capture());
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.UNPAID);

            // Verify notification was sent
            verify(notificationService).createNotification(eq(customer.getId()), any(), any(), any());
        }

        @Test
        @DisplayName("Should fail when basket is sold out")
        void createOrder_FailsWhenSoldOut() {
            // Given
            basket.setStatus(BasketStatus.SOLD_OUT);
            CreateOrderRequest request = new CreateOrderRequest(basket.getId(), 1);

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(basketService.findBasketOrThrow(basket.getId())).thenReturn(basket);

            // When/Then
            assertThatThrownBy(() -> orderService.createOrder(request, customer.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("not available");

            verify(orderRepository, never()).save(any());
            verify(basketService, never()).decrementQuantity(any(), anyInt());
        }

        @Test
        @DisplayName("Should fail when not enough quantity")
        void createOrder_FailsWhenNotEnoughQuantity() {
            // Given
            basket.setQuantityLeft(1);
            CreateOrderRequest request = new CreateOrderRequest(basket.getId(), 5);

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(basketService.findBasketOrThrow(basket.getId())).thenReturn(basket);

            // When/Then
            assertThatThrownBy(() -> orderService.createOrder(request, customer.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Not enough quantity");

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should fail when pickup window expired")
        void createOrder_FailsWhenPickupExpired() {
            // Given
            basket.setPickupEnd(Instant.now().minus(1, ChronoUnit.HOURS));
            CreateOrderRequest request = new CreateOrderRequest(basket.getId(), 1);

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(basketService.findBasketOrThrow(basket.getId())).thenReturn(basket);

            // When/Then
            assertThatThrownBy(() -> orderService.createOrder(request, customer.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expired");

            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Cancel Order Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel order and restore quantity")
        void cancelOrder_Success_RestoresQuantity() {
            // Given
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.findByOrderId(order.getId())).thenReturn(Optional.of(
                    Payment.builder().status(PaymentStatus.UNPAID).build()
            ));
            when(orderMapper.toResponse(any(Order.class))).thenReturn(new OrderResponse());

            // When
            OrderResponse response = orderService.cancelOrder(order.getId(), customer.getId());

            // Then
            assertThat(response).isNotNull();

            // Verify order status changed
            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);

            // Verify quantity was restored
            verify(basketService).incrementQuantity(eq(basket), eq(1));

            // Verify notification was sent
            verify(notificationService).createNotification(eq(customer.getId()), any(), any(), any());
        }

        @Test
        @DisplayName("Should fail cancellation when not owner")
        void cancelOrder_FailsWhenNotOwner() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            // When/Then
            assertThatThrownBy(() -> orderService.cancelOrder(order.getId(), otherUserId))
                    .isInstanceOf(ForbiddenException.class);

            verify(basketService, never()).incrementQuantity(any(), anyInt());
        }

        @Test
        @DisplayName("Should fail cancellation when too close to pickup")
        void cancelOrder_FailsWhenTooCloseToPickup() {
            // Given
            basket.setPickupStart(Instant.now().plus(10, ChronoUnit.MINUTES)); // Less than 30 min cutoff
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            // When/Then
            assertThatThrownBy(() -> orderService.cancelOrder(order.getId(), customer.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("less than");

            verify(basketService, never()).incrementQuantity(any(), anyInt());
        }

        @Test
        @DisplayName("Should fail cancellation when not reserved")
        void cancelOrder_FailsWhenNotReserved() {
            // Given
            order.setStatus(OrderStatus.PICKED_UP);
            when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

            // When/Then
            assertThatThrownBy(() -> orderService.cancelOrder(order.getId(), customer.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Only reserved");

            verify(basketService, never()).incrementQuantity(any(), anyInt());
        }
    }

    @Nested
    @DisplayName("Validate Pickup Tests")
    class ValidatePickupTests {

        @Test
        @DisplayName("Should validate pickup successfully")
        void validatePickup_Success() {
            // Given
            when(orderRepository.findByPickupCode("ABC123")).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.findByOrderId(order.getId())).thenReturn(Optional.of(
                    Payment.builder()
                            .provider(PaymentProvider.CASH)
                            .status(PaymentStatus.UNPAID)
                            .build()
            ));
            when(orderMapper.toResponse(any(Order.class))).thenReturn(new OrderResponse());
            doNothing().when(shopService).checkShopOwnership(any(), any());

            // When
            OrderResponse response = orderService.validatePickup("ABC123", merchant.getId());

            // Then
            assertThat(response).isNotNull();

            // Verify order status changed to PICKED_UP
            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.PICKED_UP);

            // Verify payment marked as paid for cash
            verify(paymentRepository).save(paymentCaptor.capture());
            assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(paymentCaptor.getValue().getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("Should fail pickup when order not reserved")
        void validatePickup_FailsWhenNotReserved() {
            // Given
            order.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findByPickupCode("ABC123")).thenReturn(Optional.of(order));
            doNothing().when(shopService).checkShopOwnership(any(), any());

            // When/Then
            assertThatThrownBy(() -> orderService.validatePickup("ABC123", merchant.getId()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("not in RESERVED");
        }
    }
}

