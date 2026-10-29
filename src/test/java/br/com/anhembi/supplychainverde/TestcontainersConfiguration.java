package br.com.anhembi.supplychainverde;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	// Mesma imagem, flags e init do docker-compose: o init cria supply_app e supply_audit.
	@Bean
	MySQLContainer mysqlContainer() {
		return new MySQLContainer("mysql:8.4")
				.withDatabaseName("supply_chain_verde")
				.withUsername("root")
				.withPassword("root")
				.withUrlParam("connectionTimeZone", "SERVER")
				.withCopyFileToContainer(MountableFile.forHostPath("docker/mysql/init"), "/docker-entrypoint-initdb.d")
				.withCommand("--default-time-zone=America/Sao_Paulo", "--character-set-server=utf8mb4",
						"--collation-server=utf8mb4_0900_as_cs");
	}

	// Sem @ServiceConnection: ele entregaria um usuário só. A API conecta como supply_app (usuário e senha de
	// application.properties) e o Flyway como root.
	@Bean
	DynamicPropertyRegistrar mysqlProperties(MySQLContainer mysql) {
		return registry -> {
			registry.add("spring.datasource.url", mysql::getJdbcUrl);
			registry.add("spring.flyway.user", mysql::getUsername);
			registry.add("spring.flyway.password", mysql::getPassword);
		};
	}

}
