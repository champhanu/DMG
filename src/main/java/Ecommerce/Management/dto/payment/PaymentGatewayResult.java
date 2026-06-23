package Ecommerce.Management.dto.payment;

public record PaymentGatewayResult(
		boolean success,
		String transactionRef,
		String failureReason
) {

	public static PaymentGatewayResult success(String transactionRef) {
		return new PaymentGatewayResult(true, transactionRef, null);
	}

	public static PaymentGatewayResult failure(String failureReason) {
		return new PaymentGatewayResult(false, null, failureReason);
	}

}
