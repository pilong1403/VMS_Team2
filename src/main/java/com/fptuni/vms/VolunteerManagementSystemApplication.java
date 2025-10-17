package com.fptuni.vms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
		org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class })
public class VolunteerManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(VolunteerManagementSystemApplication.class, args);
	}

}
