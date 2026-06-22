package Ecommerce.Management.service.tax;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TaxService {

	private final BigDecimal taxRate;

	public TaxService(@Value("${dmg.tax.rate:0.10}") BigDecimal taxRate) {
		this.taxRate = taxRate;
	}

	public BigDecimal calculateTax(BigDecimal taxableAmount) {
		return taxableAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
	}

	public BigDecimal getTaxRate() {
		return taxRate;
	}

}
