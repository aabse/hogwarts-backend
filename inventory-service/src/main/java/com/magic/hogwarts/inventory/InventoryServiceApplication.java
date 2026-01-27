package com.magic.hogwarts.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Inventory Service Spring Boot application entry point.
 *
 * <p>This service manages inventory items (wands, potions, brooms) and exposes REST endpoints under
 * {@code /api/inventory/*}.
 *
 * @author Angel Absé
 * @since 1.0
 */
@SpringBootApplication(scanBasePackages = "com.magic.hogwarts")
public class InventoryServiceApplication {

  /**
   * Application bootstrap.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(InventoryServiceApplication.class, args);
  }
}
