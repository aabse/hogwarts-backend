package com.magic.hogwarts.duel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Duel Service Spring Boot application entry point.
 *
 * <p>This service records duels between wizards and exposes REST endpoints under {@code
 * /api/duels}.
 *
 * @author Angel Absé
 * @since 1.0
 */
@SpringBootApplication(scanBasePackages = "com.magic.hogwarts")
@EnableRetry
public class DuelServiceApplication {

  /**
   * Application bootstrap.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(DuelServiceApplication.class, args);
  }
}
