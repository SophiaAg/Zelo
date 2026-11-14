package br.com.zelo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ZeloApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZeloApplication.class, args);
    }
}
