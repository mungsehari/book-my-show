package com.bookmyshow.bootstrap;

import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database setup. Connection details come from {@code spring.datasource.*}. Schema changes are
 * applied by Flyway from {@code resources/db/migration}.
 */
@EnableJpaRepositories(basePackages = "com.bookmyshow.modules")
@EnableTransactionManagement
public class DatabaseConfig {}
