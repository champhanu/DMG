package Ecommerce.Management.controller.demo;

import Ecommerce.Management.dto.demo.DemoSeedResponse;
import Ecommerce.Management.service.demo.DemoDataSeeder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/demo")
public class DemoSeedController {

	private final DemoDataSeeder demoDataSeeder;

	public DemoSeedController(DemoDataSeeder demoDataSeeder) {
		this.demoDataSeeder = demoDataSeeder;
	}

	@PostMapping("/seed")
	public DemoSeedResponse seed(@RequestParam(defaultValue = "false") boolean force) {
		return demoDataSeeder.seed(force);
	}

}
