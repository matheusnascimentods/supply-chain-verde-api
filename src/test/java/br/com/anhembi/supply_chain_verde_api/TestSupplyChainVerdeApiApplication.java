package br.com.anhembi.supply_chain_verde_api;

import org.springframework.boot.SpringApplication;

public class TestSupplyChainVerdeApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(SupplyChainVerdeApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
