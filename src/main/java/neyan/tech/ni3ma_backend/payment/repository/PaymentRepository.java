package neyan.tech.ni3ma_backend.payment.repository;

import neyan.tech.ni3ma_backend.payment.entity.Payment;
import neyan.tech.ni3ma_backend.payment.entity.PaymentProvider;
import neyan.tech.ni3ma_backend.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    List<Payment> findByStatus(PaymentStatus status);

    Page<Payment> findByProvider(PaymentProvider provider, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.order.user.id = :userId")
    Page<Payment> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PAID' AND p.paidAt BETWEEN :start AND :end")
    List<Payment> findPaidPaymentsBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :threshold")
    List<Payment> findStalePendingPayments(@Param("threshold") Instant threshold);
}

