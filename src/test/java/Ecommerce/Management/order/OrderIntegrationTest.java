package Ecommerce.Management.order;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void orderLifecycleTransitionsThroughFulfillmentAndReturn() throws Exception {
		long customerId = 6001L;
		Long productId = createProduct(createCategory("order-life"), "ORD-LIFE-001", 50.00);
		Long warehouseId = createWarehouse("WH-ORD-LIFE");
		stockInventory(warehouseId, productId, 5);
		addToCart(customerId, productId, 2);
		Long orderId = checkout(customerId);

		mockMvc.perform(get("/api/orders/" + orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMED"))
				.andExpect(jsonPath("$.allowedNextStatuses", containsInAnyOrder("PACKED", "CANCELLED")));

		mockMvc.perform(patch("/api/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "status": "PACKED" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PACKED"))
				.andExpect(jsonPath("$.allowedNextStatuses", containsInAnyOrder("SHIPPED")));

		mockMvc.perform(patch("/api/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "status": "SHIPPED" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SHIPPED"));

		mockMvc.perform(patch("/api/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "status": "DELIVERED" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DELIVERED"));

		mockMvc.perform(post("/api/orders/" + orderId + "/return")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "reason": "Changed mind"
								}
								""".formatted(customerId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RETURNED"))
				.andExpect(jsonPath("$.statusReason").value("Changed mind"))
				.andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));
	}

	@Test
	void cancelOrderReleasesReservedInventory() throws Exception {
		long customerId = 6002L;
		Long productId = createProduct(createCategory("order-cancel"), "ORD-CANCEL-001", 40.00);
		Long warehouseId = createWarehouse("WH-ORD-CANCEL");
		stockInventory(warehouseId, productId, 4);
		addToCart(customerId, productId, 2);
		Long orderId = checkout(customerId);

		mockMvc.perform(get("/api/inventory/product/" + productId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalReserved").value(2))
				.andExpect(jsonPath("$.totalAvailable").value(2));

		mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "reason": "Customer requested"
								}
								""".formatted(customerId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"))
				.andExpect(jsonPath("$.statusReason").value("Customer requested"))
				.andExpect(jsonPath("$.paymentStatus").value("REFUNDED"));

		mockMvc.perform(get("/api/inventory/product/" + productId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalReserved").value(0))
				.andExpect(jsonPath("$.totalAvailable").value(4));
	}

	@Test
	void cancelFromPackedStateIsRejected() throws Exception {
		long customerId = 6004L;
		Long productId = createProduct(createCategory("order-no-cancel-packed"), "ORD-NCP-001", 35.00);
		Long warehouseId = createWarehouse("WH-ORD-NCP");
		stockInventory(warehouseId, productId, 3);
		addToCart(customerId, productId, 1);
		Long orderId = checkout(customerId);

		mockMvc.perform(patch("/api/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "status": "PACKED" }
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "reason": "Too late"
								}
								""".formatted(customerId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Order cannot be cancelled in status: PACKED"));
	}

	@Test
	void invalidTransitionReturnsConflict() throws Exception {
		long customerId = 6003L;
		Long productId = createProduct(createCategory("order-invalid"), "ORD-INV-001", 30.00);
		Long warehouseId = createWarehouse("WH-ORD-INV");
		stockInventory(warehouseId, productId, 2);
		addToCart(customerId, productId, 1);
		Long orderId = checkout(customerId);

		mockMvc.perform(patch("/api/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "status": "DELIVERED" }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Invalid order transition from CONFIRMED to DELIVERED"));
	}

	private Long checkout(long customerId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/checkout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "paymentMethod": "CARD"
								}
								""".formatted(customerId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("CONFIRMED"))
				.andReturn();
		return JsonPath.parse(result.getResponse().getContentAsString()).read("$.orderId", Long.class);
	}

	private void addToCart(long customerId, Long productId, int quantity) throws Exception {
		mockMvc.perform(post("/api/cart/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "productId": %d,
								  "quantity": %d
								}
								""".formatted(customerId, productId, quantity)))
				.andExpect(status().isOk());
	}

	private Long createCategory(String slug) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Order Category",
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
								  "name": "Order Product",
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
