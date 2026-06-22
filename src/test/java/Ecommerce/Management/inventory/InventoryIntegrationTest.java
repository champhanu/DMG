package Ecommerce.Management.inventory;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void adminCanManageWarehousesAndInventoryAcrossLocations() throws Exception {
		Long categoryId = createCategory();
		Long productId = createProduct(categoryId, "INV-PROD-001");

		MvcResult eastResult = mockMvc.perform(post("/api/warehouses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "East Distribution Center",
								  "code": "WH-EAST",
								  "location": "New York"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("WH-EAST"))
				.andReturn();

		Long eastId = JsonPath.parse(eastResult.getResponse().getContentAsString()).read("$.id", Long.class);

		MvcResult westResult = mockMvc.perform(post("/api/warehouses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "West Distribution Center",
								  "code": "WH-WEST",
								  "location": "Los Angeles"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		Long westId = JsonPath.parse(westResult.getResponse().getContentAsString()).read("$.id", Long.class);

		stock(eastId, productId, 100);
		stock(westId, productId, 50);

		mockMvc.perform(get("/api/warehouses/" + eastId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("WH-EAST"));

		mockMvc.perform(get("/api/warehouses/" + westId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("WH-WEST"));

		mockMvc.perform(get("/api/inventory/product/" + productId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalAvailable").value(150))
				.andExpect(jsonPath("$.totalOnHand").value(150))
				.andExpect(jsonPath("$.warehouses", hasSize(2)));

		mockMvc.perform(get("/api/inventory/warehouse/" + eastId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].quantityAvailable").value(100));

		mockMvc.perform(patch("/api/inventory/adjust")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "warehouseId": %d,
								  "productId": %d,
								  "delta": -10
								}
								""".formatted(eastId, productId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantityAvailable").value(90));

		mockMvc.perform(put("/api/warehouses/" + westId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "West DC Updated",
								  "location": "San Francisco"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("West DC Updated"));

		mockMvc.perform(delete("/api/warehouses/" + westId))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/inventory/product/" + productId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.warehouses", hasSize(1)))
				.andExpect(jsonPath("$.totalAvailable").value(90));
	}

	@Test
	void rejectsDuplicateWarehouseCode() throws Exception {
		mockMvc.perform(post("/api/warehouses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Central",
								  "code": "WH-CENTRAL"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/warehouses")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Central Duplicate",
								  "code": "WH-CENTRAL"
								}
								"""))
				.andExpect(status().isConflict());
	}

	private Long createCategory() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Inventory Category",
								  "slug": "inv-cat-%d"
								}
								""".formatted(System.nanoTime())))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
	}

	private Long createProduct(Long categoryId, String sku) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Inventory Product",
								  "sku": "%s",
								  "price": 99.99,
								  "categoryId": %d
								}
								""".formatted(sku, categoryId)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
	}

	private void stock(Long warehouseId, Long productId, int quantity) throws Exception {
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
