package Ecommerce.Management.discount;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DiscountCheckoutIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void checkoutAppliesPercentagePromoCode() throws Exception {
		long customerId = 7001L;
		mockMvc.perform(post("/api/discounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "SAVE10",
								  "type": "PERCENTAGE",
								  "value": 10,
								  "active": true
								}
								"""))
				.andExpect(status().isOk());

		Long categoryId = createCategory("discount-cat");
		Long productId = createProduct(categoryId, "DISC-001", 100.00);
		Long warehouseId = createWarehouse("WH-DISC");
		stockInventory(warehouseId, productId, 5);

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

		mockMvc.perform(post("/api/checkout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "paymentMethod": "CARD",
								  "promoCode": "SAVE10"
								}
								""".formatted(customerId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.subtotal").value(200.00))
				.andExpect(jsonPath("$.discountAmount").value(20.00))
				.andExpect(jsonPath("$.promoCode").value("SAVE10"))
				.andExpect(jsonPath("$.taxAmount").value(18.00))
				.andExpect(jsonPath("$.totalAmount").value(198.00));
	}

	private Long createCategory(String slug) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Discount Category",
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
								  "name": "Discount Product",
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
