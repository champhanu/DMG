package Ecommerce.Management.order;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end component test: catalog browse → cart → checkout → fulfillment → delivered.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderEndToEndIntegrationTest {

	private static final long CUSTOMER_ID = 8001L;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void fullOrderJourneyFromCatalogBrowseToDelivered() throws Exception {
		// --- Setup catalog & inventory (admin-side) ---
		Long categoryId = createCategory("e2e-electronics", "E2E Electronics");
		Long productId = createProduct(categoryId, "E2E-LAPTOP-001", "E2E Laptop", 999.99);
		Long warehouseId = createWarehouse("WH-E2E-EAST", "E2E East DC");
		stockInventory(warehouseId, productId, 25);

		// --- 1. Customer browses catalog ---
		mockMvc.perform(get("/api/categories/" + categoryId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("E2E Electronics"));

		mockMvc.perform(get("/api/products").param("categoryId", String.valueOf(categoryId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[?(@.id == " + productId + ")].sku").value("E2E-LAPTOP-001"));

		mockMvc.perform(get("/api/products/" + productId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("E2E Laptop"))
				.andExpect(jsonPath("$.price").value(999.99));

		mockMvc.perform(get("/api/inventory/product/" + productId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalAvailable").value(25));

		// --- 2. Add to cart ---
		mockMvc.perform(post("/api/cart/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "productId": %d,
								  "quantity": 2
								}
								""".formatted(CUSTOMER_ID, productId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customerId").value(CUSTOMER_ID))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].productId").value(productId))
				.andExpect(jsonPath("$.items[0].quantity").value(2))
				.andExpect(jsonPath("$.items[0].unitPrice").value(999.99))
				.andExpect(jsonPath("$.subtotal").value(1999.98));

		mockMvc.perform(get("/api/cart").param("customerId", String.valueOf(CUSTOMER_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].quantity").value(2));

		// --- 3. Checkout → order created & confirmed ---
		MvcResult checkoutResult = mockMvc.perform(post("/api/checkout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "paymentMethod": "CARD"
								}
								""".formatted(CUSTOMER_ID)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.customerId").value(CUSTOMER_ID))
				.andExpect(jsonPath("$.status").value("CONFIRMED"))
				.andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
				.andExpect(jsonPath("$.subtotal").value(1999.98))
				.andExpect(jsonPath("$.taxAmount").value(200.00))
				.andExpect(jsonPath("$.totalAmount").value(2199.98))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].warehouseId").value(warehouseId))
				.andReturn();

		Long orderId = JsonPath.parse(checkoutResult.getResponse().getContentAsString())
				.read("$.orderId", Long.class);

		// Cart cleared after checkout
		mockMvc.perform(get("/api/cart").param("customerId", String.valueOf(CUSTOMER_ID)))
				.andExpect(status().isNotFound());

		// Inventory reserved
		mockMvc.perform(get("/api/inventory/product/" + productId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalReserved").value(2))
				.andExpect(jsonPath("$.totalAvailable").value(23));

		// --- 4. Verify order & payment ---
		mockMvc.perform(get("/api/orders/" + orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMED"))
				.andExpect(jsonPath("$.allowedNextStatuses", containsInAnyOrder("PACKED", "CANCELLED")))
				.andExpect(jsonPath("$.items[0].productName").value("E2E Laptop"))
				.andExpect(jsonPath("$.items[0].sku").value("E2E-LAPTOP-001"))
				.andExpect(jsonPath("$.totalAmount").value(2199.98));

		mockMvc.perform(get("/api/orders").param("customerId", String.valueOf(CUSTOMER_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(orderId))
				.andExpect(jsonPath("$[0].status").value("CONFIRMED"));

		mockMvc.perform(get("/api/payments/order/" + orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUCCESS"))
				.andExpect(jsonPath("$.amount").value(2199.98));

		awaitFulfillmentTasks(orderId);

		// --- 5. Warehouse staff: CONFIRMED → PACKED ---
		mockMvc.perform(patch("/api/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "status": "PACKED" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PACKED"))
				.andExpect(jsonPath("$.allowedNextStatuses", containsInAnyOrder("SHIPPED")));

		// --- 6. Warehouse staff: PACKED → SHIPPED ---
		mockMvc.perform(patch("/api/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "status": "SHIPPED" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SHIPPED"))
				.andExpect(jsonPath("$.allowedNextStatuses", containsInAnyOrder("DELIVERED")));

		// --- 7. Warehouse staff: SHIPPED → DELIVERED ---
		mockMvc.perform(patch("/api/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "status": "DELIVERED" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DELIVERED"))
				.andExpect(jsonPath("$.allowedNextStatuses", containsInAnyOrder("RETURNED")));

		// --- 8. Final assertions ---
		mockMvc.perform(get("/api/orders/" + orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DELIVERED"))
				.andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));

		mockMvc.perform(get("/api/fulfillment/orders/" + orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].warehouseId").value(warehouseId))
				.andExpect(jsonPath("$[0].productId").value(productId))
				.andExpect(jsonPath("$[0].quantity").value(2));
	}

	private void awaitFulfillmentTasks(Long orderId) throws Exception {
		for (int attempt = 0; attempt < 50; attempt++) {
			MvcResult result = mockMvc.perform(get("/api/fulfillment/orders/" + orderId))
					.andExpect(status().isOk())
					.andReturn();
			int count = JsonPath.parse(result.getResponse().getContentAsString()).read("$.length()", Integer.class);
			if (count >= 1) {
				return;
			}
			Thread.sleep(100);
		}
		throw new AssertionError("Fulfillment tasks were not created for order " + orderId);
	}

	private Long createCategory(String slug, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "%s",
								  "slug": "%s"
								}
								""".formatted(name, slug + "-" + System.nanoTime())))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
	}

	private Long createProduct(Long categoryId, String sku, String name, double price) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "%s",
								  "sku": "%s",
								  "price": %.2f,
								  "categoryId": %d
								}
								""".formatted(name, sku, price, categoryId)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
	}

	private Long createWarehouse(String code, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/warehouses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "%s",
								  "code": "%s",
								  "location": "Test City"
								}
								""".formatted(name, code + "-" + System.nanoTime())))
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
