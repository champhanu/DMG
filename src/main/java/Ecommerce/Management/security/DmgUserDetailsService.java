package Ecommerce.Management.security;

import Ecommerce.Management.repository.security.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DmgUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public DmgUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userRepository.findByUsername(username)
				.map(DmgUserDetails::new)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
	}

}
