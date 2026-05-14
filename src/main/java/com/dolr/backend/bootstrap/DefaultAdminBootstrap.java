package com.dolr.backend.bootstrap;

import com.dolr.backend.entity.User;
import com.dolr.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates one default {@code ADMIN} user when the database has no administrator yet.
 * Runs once per environment; change {@code app.bootstrap-admin.*} or disable after first login.
 */
@Component
@Order(1000)
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DefaultAdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DefaultAdminBootstrap.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.bootstrap-admin.email:admin@dolr.gov.in}")
	private String adminEmail;

	@Value("${app.bootstrap-admin.password:Admin@123}")
	private String adminPassword;

	@Value("${app.bootstrap-admin.full-name:admin}")
	private String adminFullName;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (userRepository.existsByRole("ADMIN")) {
			return;
		}
		if (userRepository.findByEmailIgnoreCase(adminEmail.trim().toLowerCase()).isPresent()) {
			log.warn("Bootstrap admin skipped: email {} already exists (no ADMIN role yet — fix data manually).", adminEmail);
			return;
		}
		if (adminPassword == null || adminPassword.length() < 6) {
			log.error("Bootstrap admin skipped: app.bootstrap-admin.password must be at least 6 characters.");
			return;
		}

		User admin = User.builder()
				.fullName(adminFullName)
				.email(adminEmail.trim().toLowerCase())
				.mobileNumber("0000000000")
				.department("—")
				.division("—")
				.designation("Administrator")
				.password(passwordEncoder.encode(adminPassword))
				.role("ADMIN")
				.approved(true)
				.isOfficer(false)
				.reportingOfficer(null)
				.build();

		userRepository.save(admin);
		log.warn("Default ADMIN user created (email: {}). Sign in via Admin login and change this password.", adminEmail);
	}
}
