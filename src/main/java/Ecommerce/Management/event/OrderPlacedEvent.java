package Ecommerce.Management.event;

public record OrderPlacedEvent(Long orderId, Long customerId) {
}
