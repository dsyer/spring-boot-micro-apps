package com.example.demo;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

@RunWith(SpringRunner.class)
@SpringBootTest
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
