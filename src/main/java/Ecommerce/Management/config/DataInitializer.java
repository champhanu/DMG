package Ecommerce.Management.config;

import Ecommerce.Management.domain.security.Role;
import Ecommerce.Management.domain.security.User;
import Ecommerce.Management.repository.security.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

	@Bean
	CommandLineRunner seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			seedIfMissing(userRepository, passwordEncoder, "admin", "admin123", Role.ADMIN, null);
			seedIfMissing(userRepository, passwordEncoder, "staff", "staff123", Role.WAREHOUSE_STAFF, null);
			seedIfMissing(userRepository, passwordEncoder, "customer", "customer123", Role.CUSTOMER, 1L);
		};
	}

	private void seedIfMissing(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			String username,
			String rawPassword,
			Role role,
			Long customerId) {
		if (userRepository.existsByUsername(username)) {
			return;
		}
		User user = new User();
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(rawPassword));
		user.setRole(role);
		user.setCustomerId(customerId);
		userRepository.save(user);
	}

}
