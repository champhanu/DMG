package Ecommerce.Management.service.discount;

import Ecommerce.Management.domain.discount.Discount;
import Ecommerce.Management.domain.discount.DiscountType;
import Ecommerce.Management.dto.discount.DiscountRequest;
import Ecommerce.Management.dto.discount.DiscountResponse;
import Ecommerce.Management.exception.DuplicateResourceException;
import Ecommerce.Management.exception.InvalidOperationException;
import Ecommerce.Management.exception.ResourceNotFoundException;
import Ecommerce.Management.repository.discount.DiscountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class DiscountService {

	private final DiscountRepository discountRepository;

	public DiscountService(DiscountRepository discountRepository) {
		this.discountRepository = discountRepository;
	}

	public DiscountResponse create(DiscountRequest request) {
		if (discountRepository.findByCodeIgnoreCase(request.code()).isPresent()) {
			throw new DuplicateResourceException("Discount code already exists: " + request.code());
		}
		return toResponse(discountRepository.save(fromRequest(new Discount(), request)));
	}

	public DiscountResponse update(Long id, DiscountRequest request) {
		Discount discount = findDiscount(id);
		discountRepository.findByCodeIgnoreCase(request.code())
				.filter(existing -> !existing.getId().equals(id))
				.ifPresent(existing -> {
					throw new DuplicateResourceException("Discount code already exists: " + request.code());
				});
		return toResponse(discountRepository.save(fromRequest(discount, request)));
	}

	@Transactional(readOnly = true)
	public DiscountResponse get(Long id) {
		return toResponse(findDiscount(id));
	}

	@Transactional(readOnly = true)
	public List<DiscountResponse> listActive() {
		return discountRepository.findByActiveTrueOrderByCodeAsc().stream()
				.map(this::toResponse)
				.toList();
	}

	public AppliedDiscount applyPromoCode(String promoCode, BigDecimal subtotal) {
		if (promoCode == null || promoCode.isBlank()) {
			return AppliedDiscount.none();
		}

		Discount discount = discountRepository.findByCodeIgnoreCase(promoCode.trim())
				.orElseThrow(() -> new InvalidOperationException("Invalid promo code: " + promoCode));

		if (!discount.isActive()) {
			throw new InvalidOperationException("Promo code is inactive: " + promoCode);
		}
		if (discount.getMaxUses() != null && discount.getUsedCount() >= discount.getMaxUses()) {
			throw new InvalidOperationException("Promo code usage limit reached: " + promoCode);
		}
		if (discount.getMinOrderAmount() != null && subtotal.compareTo(discount.getMinOrderAmount()) < 0) {
			throw new InvalidOperationException(
					"Order subtotal does not meet minimum for promo code: " + promoCode);
		}

		BigDecimal discountAmount = calculateDiscountAmount(discount, subtotal);
		discount.setUsedCount(discount.getUsedCount() + 1);
		return new AppliedDiscount(discount.getCode(), discountAmount);
	}

	private BigDecimal calculateDiscountAmount(Discount discount, BigDecimal subtotal) {
		BigDecimal amount = switch (discount.getType()) {
			case PERCENTAGE -> subtotal.multiply(discount.getValue())
					.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
			case FIXED_AMOUNT -> discount.getValue();
		};
		if (amount.compareTo(subtotal) > 0) {
			return subtotal;
		}
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	private Discount findDiscount(Long id) {
		return discountRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Discount not found: " + id));
	}

	private Discount fromRequest(Discount discount, DiscountRequest request) {
		discount.setCode(request.code().trim().toUpperCase());
		discount.setType(request.type());
		discount.setValue(request.value());
		discount.setMinOrderAmount(request.minOrderAmount());
		discount.setMaxUses(request.maxUses());
		discount.setActive(request.active());
		return discount;
	}

	private DiscountResponse toResponse(Discount discount) {
		return new DiscountResponse(
				discount.getId(),
				discount.getCode(),
				discount.getType(),
				discount.getValue(),
				discount.getMinOrderAmount(),
				discount.getMaxUses(),
				discount.getUsedCount(),
				discount.isActive(),
				discount.getCreatedAt(),
				discount.getUpdatedAt());
	}

	public record AppliedDiscount(String promoCode, BigDecimal discountAmount) {

		public static AppliedDiscount none() {
			return new AppliedDiscount(null, BigDecimal.ZERO);
		}

	}

}
