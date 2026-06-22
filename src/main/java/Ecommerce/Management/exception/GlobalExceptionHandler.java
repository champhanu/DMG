package Ecommerce.Management.exception;

import Ecommerce.Management.dto.common.ApiError;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(InsufficientInventoryException.class)
	public ResponseEntity<ApiError> handleInsufficientInventory(
			InsufficientInventoryException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of(409, "Conflict", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(PaymentFailedException.class)
	public ResponseEntity<ApiError> handlePaymentFailed(PaymentFailedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
				.body(ApiError.of(402, "Payment Required", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler({ OptimisticLockingFailureException.class, OptimisticLockException.class })
	public ResponseEntity<ApiError> handleOptimisticLock(Exception ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of(409, "Conflict",
						"Order was modified by another operation. Please refresh and retry.",
						request.getRequestURI()));
	}

	@ExceptionHandler(InvalidOrderStateTransitionException.class)
	public ResponseEntity<ApiError> handleInvalidTransition(
			InvalidOrderStateTransitionException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of(409, "Conflict", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiError.of(404, "Not Found", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of(409, "Conflict", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(InvalidOperationException.class)
	public ResponseEntity<ApiError> handleInvalidOperation(InvalidOperationException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiError.of(400, "Bad Request", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toFieldError)
				.toList();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiError.of(400, "Bad Request", "Validation failed", request.getRequestURI(), fieldErrors));
	}

	private ApiError.FieldError toFieldError(FieldError fieldError) {
		return new ApiError.FieldError(fieldError.getField(), fieldError.getDefaultMessage());
	}

}
