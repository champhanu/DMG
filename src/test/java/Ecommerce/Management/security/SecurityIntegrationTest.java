package Ecommerce.Management.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "dmg.security.enforce-rbac=true")
class SecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void unauthenticatedCatalogBrowseIsRejected() throws Exception {
		mockMvc.perform(get("/api/categories"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void customerCannotCreateWarehouse() throws Exception {
		mockMvc.perform(post("/api/warehouses")
						.with(httpBasic("customer", "customer123"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Blocked Warehouse",
								  "code": "WH-BLOCKED",
								  "location": "Nowhere"
								}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanCreateDiscount() throws Exception {
		mockMvc.perform(post("/api/discounts")
						.with(httpBasic("admin", "admin123"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "code": "ADMIN10-%d",
								  "type": "PERCENTAGE",
								  "value": 10,
								  "active": true
								}
								""".formatted(System.nanoTime())))
				.andExpect(status().isOk());
	}

}
