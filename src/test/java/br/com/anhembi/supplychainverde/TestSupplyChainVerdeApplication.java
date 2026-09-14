package br.com.anhembi.supplychainverde;

import org.springframework.boot.SpringApplication;

public class TestSupplyChainVerdeApplication {

	public static void main(String[] args) {
		SpringApplication.from(SupplyChainVerdeApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
