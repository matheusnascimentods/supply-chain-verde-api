package br.com.anhembi.supplychainverde;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.TimeZone;

@SpringBootApplication
@EnableAspectJAutoProxy
public class SupplyChainVerdeApplication {

	// Fuso de negócio: LocalDateTime.now(), leitura de TIMESTAMPTZ pelo Hibernate e JSON.
	// Bloco estático para valer também nos testes, que não passam pelo main.
	static {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
	}

	public static void main(String[] args) {
		SpringApplication.run(SupplyChainVerdeApplication.class, args);
	}

}
