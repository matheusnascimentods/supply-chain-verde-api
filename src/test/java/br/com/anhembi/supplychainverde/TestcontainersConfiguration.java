package br.com.anhembi.supplychainverde;

import java.nio.file.Path;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	// Mesma imagem do docker-compose: a migration do pg_cron precisa da extensão.
	private static final String IMAGE = new ImageFromDockerfile("supply-chain-verde-postgres", false)
			.withDockerfile(Path.of("docker/postgres/Dockerfile"))
			.get();

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres"))
				.withDatabaseName("supply_chain_verde")
				.withCommand("postgres", "-c", "fsync=off",
						"-c", "shared_preload_libraries=pg_cron", "-c", "cron.database_name=supply_chain_verde");
	}

}
