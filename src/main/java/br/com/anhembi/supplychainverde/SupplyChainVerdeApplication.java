package br.com.anhembi.supplychainverde;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class SupplyChainVerdeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SupplyChainVerdeApplication.class, args);
	}

}
