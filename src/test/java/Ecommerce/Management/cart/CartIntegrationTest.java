package Ecommerce.Management.cart;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CartIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void customerCanAddUpdateAndRemoveCartItems() throws Exception {
		Long categoryId = createCategory();
		Long productId = createProduct(categoryId, "CART-PHONE-001", 499.99);
		long customerId = 1001L;

		mockMvc.perform(get("/api/cart").param("customerId", String.valueOf(customerId)))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/cart/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "productId": %d,
								  "quantity": 2
								}
								""".formatted(customerId, productId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(2))
				.andExpect(jsonPath("$.subtotal").value(999.98))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].unitPrice").value(499.99));

		mockMvc.perform(post("/api/cart/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "productId": %d,
								  "quantity": 1
								}
								""".formatted(customerId, productId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(3))
				.andExpect(jsonPath("$.items", hasSize(1)));

		MvcResult cartResult = mockMvc.perform(get("/api/cart").param("customerId", String.valueOf(customerId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customerId").value(customerId))
				.andReturn();

		Long itemId = JsonPath.parse(cartResult.getResponse().getContentAsString())
				.read("$.items[0].id", Long.class);

		mockMvc.perform(put("/api/cart/items/" + itemId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "quantity": 1
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.subtotal").value(499.99));

		mockMvc.perform(delete("/api/cart/items/" + itemId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(0)))
				.andExpect(jsonPath("$.subtotal").value(0));

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

		mockMvc.perform(delete("/api/cart").param("customerId", String.valueOf(customerId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	void cartSupportsMultipleProductsAcrossCategories() throws Exception {
		Long electronicsId = createCategory("electronics-multi");
		Long fashionId = createCategory("fashion-multi");
		Long phoneId = createProduct(electronicsId, "MULTI-PHONE-001", "DMG Phone", 499.99);
		Long caseId = createProduct(electronicsId, "MULTI-CASE-001", "Phone Case", 19.99);
		Long shirtId = createProduct(fashionId, "MULTI-SHIRT-001", "DMG T-Shirt", 29.99);
		long customerId = 3001L;

		mockMvc.perform(post("/api/cart/items/bulk")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "items": [
								    { "productId": %d, "quantity": 2 },
								    { "productId": %d, "quantity": 1 },
								    { "productId": %d, "quantity": 3 }
								  ]
								}
								""".formatted(customerId, phoneId, caseId, shirtId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lineCount").value(3))
				.andExpect(jsonPath("$.totalItems").value(6))
				.andExpect(jsonPath("$.items", hasSize(3)))
				.andExpect(jsonPath("$.subtotal").value(1109.94));

		mockMvc.perform(post("/api/cart/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": %d,
								  "productId": %d,
								  "quantity": 1
								}
								""".formatted(customerId, phoneId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lineCount").value(3))
				.andExpect(jsonPath("$.totalItems").value(7))
				.andExpect(jsonPath("$.items[?(@.sku == 'MULTI-PHONE-001')].quantity").value(3));
	}

	@Test
	void rejectsInactiveProductInCart() throws Exception {
		Long categoryId = createCategory();
		Long productId = createProduct(categoryId, "CART-INACTIVE-001", 99.99);

		mockMvc.perform(delete("/api/products/" + productId))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/cart/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "customerId": 2001,
								  "productId": %d,
								  "quantity": 1
								}
								""".formatted(productId)))
				.andExpect(status().isNotFound());
	}

	private Long createCategory(String slugSuffix) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Cart Test Category %s",
								  "slug": "%s"
								}
								""".formatted(slugSuffix, slugSuffix + "-" + System.nanoTime())))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
	}

	private Long createCategory() throws Exception {
		return createCategory("cart-test");
	}

	private Long createProduct(Long categoryId, String sku, double price) throws Exception {
		return createProduct(categoryId, sku, "Cart Test Product", price);
	}

	private Long createProduct(Long categoryId, String sku, String name, double price) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "%s",
								  "sku": "%s",
								  "description": "For cart tests",
								  "price": %.2f,
								  "categoryId": %d
								}
								""".formatted(name, sku, price, categoryId)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
	}

}
