package Ecommerce.Management.payment;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void customerCanViewPaymentsAndProcessRefund() throws Exception {
		long customerId = 6001L;
		Long categoryId = createCategory();
		Long productId = createProduct(categoryId, "PAY-PROD-001", 50.00);
		Long warehouseId = createWarehouse();
		stockInventory(warehouseId, productId, 5);

		mockMvc.perform(post("/api/cart/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "productId": %d,
								  "quantity": 1
								}
								""".formatted(customerId, productId)))
				.andExpect(status().isOk());

		MvcResult checkoutResult = mockMvc.perform(post("/api/checkout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "paymentMethod": "UPI"
								}
								""".formatted(customerId)))
				.andExpect(status().isCreated())
				.andReturn();

		Long orderId = JsonPath.parse(checkoutResult.getResponse().getContentAsString())
				.read("$.orderId", Long.class);

		MvcResult paymentResult = mockMvc.perform(get("/api/payments/order/" + orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUCCESS"))
				.andExpect(jsonPath("$.paymentMethod").value("UPI"))
				.andExpect(jsonPath("$.amount").value(55.00))
				.andReturn();

		Long paymentId = JsonPath.parse(paymentResult.getResponse().getContentAsString())
				.read("$.id", Long.class);

		mockMvc.perform(get("/api/payments").param("customerId", String.valueOf(customerId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].transactionRef").exists());

		mockMvc.perform(post("/api/payments/" + paymentId + "/refund")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amount": 25.00,
								  "reason": "Partial return"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PARTIALLY_REFUNDED"))
				.andExpect(jsonPath("$.refundedAmount").value(25.00));

		mockMvc.perform(post("/api/payments/" + paymentId + "/refund")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amount": 30.00,
								  "reason": "Remaining refund"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REFUNDED"))
				.andExpect(jsonPath("$.refundedAmount").value(55.00));
	}

	@Test
	void rejectsUnsupportedPaymentMethodAtCheckout() throws Exception {
		long customerId = 6002L;
		Long categoryId = createCategory();
		Long productId = createProduct(categoryId, "PAY-BAD-001", 10.00);
		Long warehouseId = createWarehouse();
		stockInventory(warehouseId, productId, 2);

		mockMvc.perform(post("/api/cart/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "productId": %d,
								  "quantity": 1
								}
								""".formatted(customerId, productId)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/checkout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "paymentMethod": "BITCOIN"
								}
								""".formatted(customerId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Unsupported payment method: BITCOIN"));
	}

	private Long createCategory() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Payment Category",
								  "slug": "pay-cat-%d"
								}
								""".formatted(System.nanoTime())))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
	}

	private Long createProduct(Long categoryId, String sku, double price) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Payment Product",
								  "sku": "%s",
								  "price": %.2f,
								  "categoryId": %d
								}
								""".formatted(sku, price, categoryId)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
	}

	private Long createWarehouse() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/warehouses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Payment WH",
								  "code": "PWH-%d"
								}
								""".formatted(System.nanoTime())))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
	}

	private void stockInventory(Long warehouseId, Long productId, int quantity) throws Exception {
		mockMvc.perform(post("/api/inventory")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "warehouseId": %d,
								  "productId": %d,
								  "quantity": %d
								}
								""".formatted(warehouseId, productId, quantity)))
				.andExpect(status().isCreated());
	}

}
