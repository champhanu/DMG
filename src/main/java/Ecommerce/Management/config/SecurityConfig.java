package Ecommerce.Management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Value("${dmg.security.enforce-rbac:true}")
	private boolean enforceRbac;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		if (!enforceRbac) {
			http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
		}
		else {
			http.httpBasic(basic -> {})
					.authorizeHttpRequests(auth -> auth
							.requestMatchers("/actuator/health", "/actuator/info").permitAll()
							.requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/products/**").authenticated()
							.requestMatchers(HttpMethod.POST, "/api/categories/**", "/api/products/**").hasRole("ADMIN")
							.requestMatchers(HttpMethod.PUT, "/api/categories/**", "/api/products/**").hasRole("ADMIN")
							.requestMatchers(HttpMethod.DELETE, "/api/categories/**", "/api/products/**").hasRole("ADMIN")
							.requestMatchers("/api/admin/**").hasRole("ADMIN")
							.requestMatchers("/api/warehouses/**").hasRole("ADMIN")
							.requestMatchers("/api/inventory/**").hasAnyRole("ADMIN", "WAREHOUSE_STAFF")
							.requestMatchers("/api/discounts/**").hasRole("ADMIN")
							.requestMatchers("/api/cart/**", "/api/checkout").hasRole("CUSTOMER")
							.requestMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole("CUSTOMER", "WAREHOUSE_STAFF", "ADMIN")
							.requestMatchers(HttpMethod.POST, "/api/orders/*/cancel", "/api/orders/*/return").hasRole("CUSTOMER")
							.requestMatchers(HttpMethod.PATCH, "/api/orders/*/status").hasAnyRole("WAREHOUSE_STAFF", "ADMIN")
							.requestMatchers("/api/fulfillment/**").hasAnyRole("WAREHOUSE_STAFF", "ADMIN")
							.requestMatchers(HttpMethod.GET, "/api/payments/**").hasAnyRole("CUSTOMER", "ADMIN")
							.requestMatchers(HttpMethod.POST, "/api/payments/**").hasAnyRole("ADMIN", "CUSTOMER")
							.anyRequest().authenticated());
		}
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
