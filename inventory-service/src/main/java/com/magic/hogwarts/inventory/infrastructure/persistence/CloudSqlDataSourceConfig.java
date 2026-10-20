package com.magic.hogwarts.inventory.infrastructure.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configures the PostgreSQL {@link DataSource} on Cloud SQL using the Cloud SQL Java Connector
 * (JDBC Socket Factory). Active only under the {@code gcp} profile; without it, Spring Boot keeps
 * auto-configuring H2 as before.
 *
 * @since 1.0
 */
@Configuration
@Profile("gcp")
public class CloudSqlDataSourceConfig {

  /** Database user ({@code hogwarts.datasource.cloud-sql.username}). */
  @Value("${hogwarts.datasource.cloud-sql.username}")
  private String username;

  /** Database user password ({@code hogwarts.datasource.cloud-sql.password}). */
  @Value("${hogwarts.datasource.cloud-sql.password}")
  private String password;

  /** Database name ({@code hogwarts.datasource.cloud-sql.db-name}). */
  @Value("${hogwarts.datasource.cloud-sql.db-name}")
  private String dbName;

  /**
   * Cloud SQL instance connection name, in {@code PROJECT_ID:REGION:INSTANCE_ID} format ({@code
   * hogwarts.datasource.cloud-sql.instance-connection-name}).
   */
  @Value("${hogwarts.datasource.cloud-sql.instance-connection-name}")
  private String instanceConnectionName;

  /**
   * Creates the {@link DataSource} that connects to Cloud SQL through the PostgreSQL socket
   * factory, using the values read from this profile's properties.
   *
   * @return a {@link HikariDataSource} configured with the Cloud SQL Java Connector
   */
  @Bean
  public DataSource dataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:postgresql:///" + dbName);
    config.setUsername(username);
    config.setPassword(password);
    config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.postgres.SocketFactory");
    config.addDataSourceProperty("cloudSqlInstance", instanceConnectionName);
    return new HikariDataSource(config);
  }
}
