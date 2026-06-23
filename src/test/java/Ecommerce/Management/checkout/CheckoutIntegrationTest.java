package Ecommerce.Management.checkout;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckoutIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void checkoutAtomicallyReservesInventoryProcessesPaymentAndCreatesOrder() throws Exception {
		long customerId = 5001L;
		Long categoryId = createCategory("checkout-cat");
		Long productId = createProduct(categoryId, "CHK-PROD-001", 100.00);
		Long warehouseId = createWarehouse("WH-EAST");
		stockInventory(warehouseId, productId, 10);

		mockMvc.perform(post("/api/cart/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "productId": %d,
								  "quantity": 2
								}
								""".formatted(customerId, productId)))
				.andExpect(status().isOk());

		MvcResult checkoutResult = mockMvc.perform(post("/api/checkout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "paymentMethod": "CARD"
								}
								""".formatted(customerId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("CONFIRMED"))
				.andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
				.andExpect(jsonPath("$.subtotal").value(200.00))
				.andExpect(jsonPath("$.taxAmount").value(20.00))
				.andExpect(jsonPath("$.totalAmount").value(220.00))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andReturn();

		Long orderId = JsonPath.parse(checkoutResult.getResponse().getContentAsString())
				.read("$.orderId", Long.class);

		mockMvc.perform(get("/api/cart").param("customerId", String.valueOf(customerId)))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/orders").param("customerId", String.valueOf(customerId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(orderId))
				.andExpect(jsonPath("$[0].status").value("CONFIRMED"));

		mockMvc.perform(get("/api/orders/" + orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cartId").exists())
				.andExpect(jsonPath("$.items[0].warehouseId").value(warehouseId));
	}

	@Test
	void checkoutFailsWhenInventoryInsufficient() throws Exception {
		long customerId = 5002L;
		Long categoryId = createCategory("checkout-low-stock");
		Long productId = createProduct(categoryId, "CHK-LOW-001", 50.00);
		Long warehouseId = createWarehouse("WH-WEST");
		stockInventory(warehouseId, productId, 1);

		mockMvc.perform(post("/api/cart/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "productId": %d,
								  "quantity": 3
								}
								""".formatted(customerId, productId)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/checkout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d
								}
								""".formatted(customerId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message", containsString("Insufficient inventory")));
	}

	@Test
	void paymentFailureRollsBackCheckout() throws Exception {
		long customerId = 5003L;
		Long categoryId = createCategory("checkout-pay-fail");
		Long productId = createProduct(categoryId, "CHK-PAY-001", 25.00);
		Long warehouseId = createWarehouse("WH-NORTH");
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

		mockMvc.perform(post("/api/checkout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "simulatePaymentFailure": true
								}
								""".formatted(customerId)))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.message").value("Payment declined by gateway"));

		mockMvc.perform(get("/api/cart").param("customerId", String.valueOf(customerId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	private Long createCategory(String slug) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Checkout Category",
								  "slug": "%s"
								}
								""".formatted(slug + "-" + System.nanoTime())))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
	}

	private Long createProduct(Long categoryId, String sku, double price) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Checkout Product",
								  "sku": "%s",
								  "price": %.2f,
								  "categoryId": %d
								}
								""".formatted(sku, price, categoryId)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
	}

	private Long createWarehouse(String code) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/warehouses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Warehouse %s",
								  "code": "%s",
								  "location": "Test City"
								}
								""".formatted(code, code + "-" + System.nanoTime())))
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
