package Ecommerce.Management.checkout;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConcurrentCheckoutIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void concurrentCheckoutsDoNotOversellSingleUnit() throws Exception {
		long customerA = 9001L;
		long customerB = 9002L;
		Long productId = createProduct(createCategory("concurrent"), "CONC-001", 100.00);
		Long warehouseId = createWarehouse("WH-CONC");
		stockInventory(warehouseId, productId, 1);

		addToCart(customerA, productId, 1);
		addToCart(customerB, productId, 1);

		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger conflictCount = new AtomicInteger();

		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			List<Future<?>> futures = new ArrayList<>();
			for (long customerId : List.of(customerA, customerB)) {
				futures.add(executor.submit(() -> {
					try {
						start.await();
						int status = mockMvc.perform(post("/api/checkout")
										.contentType(MediaType.APPLICATION_JSON)
										.content("""
												{
												  "customerId": %d,
												  "paymentMethod": "CARD"
												}
												""".formatted(customerId)))
								.andReturn()
								.getResponse()
								.getStatus();
						if (status == 201) {
							successCount.incrementAndGet();
						}
						else if (status == 409) {
							conflictCount.incrementAndGet();
						}
					}
					catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}));
			}
			start.countDown();
			for (Future<?> future : futures) {
				future.get(30, TimeUnit.SECONDS);
			}
		}

		assertThat(successCount.get()).isEqualTo(1);
		assertThat(conflictCount.get()).isEqualTo(1);
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
								  "name": "Concurrent Category",
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
								  "name": "Concurrent Product",
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
