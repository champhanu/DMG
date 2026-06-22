package Ecommerce.Management.controller.fulfillment;

import Ecommerce.Management.domain.fulfillment.FulfillmentTask;
import Ecommerce.Management.service.fulfillment.FulfillmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fulfillment")
public class FulfillmentController {

	private final FulfillmentService fulfillmentService;

	public FulfillmentController(FulfillmentService fulfillmentService) {
		this.fulfillmentService = fulfillmentService;
	}

	@GetMapping("/orders/{orderId}")
	public List<FulfillmentTask> getTasksForOrder(@PathVariable Long orderId) {
		return fulfillmentService.getTasksForOrder(orderId);
	}

}
