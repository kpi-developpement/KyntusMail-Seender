package com.seender.mail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling // Zdnaha bach n-ktiviw l'chronomètre (Queue processing)
public class MailApplication {

	public static void main(String[] args) {
		SpringApplication.run(MailApplication.class, args);
	}

}