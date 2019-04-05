package com.example.demo;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(classes = DemoApplication.class)
@DirtiesContext
@AutoConfigureWebTestClient
public class DemoApplicationTests {

	@Autowired
	private WebTestClient client;

	@Test
	public void get() throws Exception {
		client.get().uri("/").exchange().expectBody(String.class).isEqualTo("Hello");
	}

}
