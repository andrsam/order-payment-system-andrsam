package dev.sorokin.mapper;

import dev.sorokin.api.dto.OrderCreateRequestDto;
import dev.sorokin.api.dto.OrderResponseDto;
import dev.sorokin.domain.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderResponseDto toOrderResponseDto(Order order);

    @Mapping(target = "paymentStatus", constant = "NEW")
    @Mapping(target = "customerId", expression = "java(java.util.concurrent.ThreadLocalRandom.current().nextLong())")
    Order toOrder(OrderCreateRequestDto requestDto);
}
