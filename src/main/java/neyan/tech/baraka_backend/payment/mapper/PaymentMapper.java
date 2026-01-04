package neyan.tech.baraka_backend.payment.mapper;

import neyan.tech.baraka_backend.payment.dto.CreatePaymentRequest;
import neyan.tech.baraka_backend.payment.dto.PaymentResponse;
import neyan.tech.baraka_backend.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Payment toEntity(CreatePaymentRequest request);

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "amount", source = "order.totalPrice")
    @Mapping(target = "currency", source = "order.basket.currency")
    PaymentResponse toResponse(Payment payment);

    List<PaymentResponse> toResponseList(List<Payment> payments);
}

