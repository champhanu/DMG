package Ecommerce.Management.catalog;

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
class CatalogIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void adminCanManageCatalogAndCustomerCanBrowseActiveItems() throws Exception {
		MvcResult electronicsResult = mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Electronics",
								  "slug": "electronics",
								  "description": "Electronic devices"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.slug").value("electronics"))
				.andReturn();

		String electronicsBody = electronicsResult.getResponse().getContentAsString();
		Long electronicsId = JsonPath.parse(electronicsBody).read("$.id", Long.class);

		mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Phones",
								  "slug": "phones",
								  "description": "Mobile phones",
								  "parentId": %d
								}
								""".formatted(electronicsId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.parentId").value(electronicsId));

		mockMvc.perform(get("/api/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].slug").value("electronics"));

		mockMvc.perform(get("/api/categories").param("parentId", electronicsId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].slug").value("phones"));

		MvcResult productResult = mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "DMG Phone X",
								  "sku": "PHONE-X-001",
								  "description": "Flagship phone",
								  "price": 699.99,
								  "categoryId": %d
								}
								""".formatted(electronicsId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sku").value("PHONE-X-001"))
				.andExpect(jsonPath("$.categoryName").value("Electronics"))
				.andReturn();

		String productBody = productResult.getResponse().getContentAsString();
		Long productId = JsonPath.parse(productBody).read("$.id", Long.class);

		mockMvc.perform(get("/api/products").param("search", "DMG"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].name").value("DMG Phone X"));

		mockMvc.perform(get("/api/products/" + productId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.price").value(699.99));

		mockMvc.perform(put("/api/products/" + productId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "DMG Phone X Pro",
								  "sku": "PHONE-X-001",
								  "description": "Updated flagship phone",
								  "price": 799.99,
								  "categoryId": %d
								}
								""".formatted(electronicsId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("DMG Phone X Pro"))
				.andExpect(jsonPath("$.price").value(799.99));

		mockMvc.perform(delete("/api/products/" + productId))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(0)));

		mockMvc.perform(get("/api/products").param("includeInactive", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].active").value(false));
	}

	@Test
	void rejectsDuplicateCategorySlug() throws Exception {
		mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Fashion",
								  "slug": "fashion"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Fashion Duplicate",
								  "slug": "fashion"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Category slug already exists: fashion"));
	}

	@Test
	void rejectsInvalidProductPayload() throws Exception {
		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "",
								  "sku": "bad sku",
								  "price": 0,
								  "categoryId": 999
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors").isArray());
	}

}
