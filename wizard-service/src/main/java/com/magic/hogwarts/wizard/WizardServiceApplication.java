package com.magic.hogwarts.wizard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Wizard Service Spring Boot application entry point.
 *
 * <p>This service manages wizards, their houses and levels, and exposes REST endpoints under {@code
 * /api/wizards}.
 *
 * @author Angel Absé
 * @since 1.0
 */
@SpringBootApplication(scanBasePackages = "com.magic.hogwarts")
@EnableRetry
public class WizardServiceApplication {

  /**
   * Application bootstrap.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(WizardServiceApplication.class, args);
  }
}
