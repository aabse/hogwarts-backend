package com.magic.hogwarts.inventory.infrastructure.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configura el {@link DataSource} de PostgreSQL en Cloud SQL usando el Cloud SQL Java Connector
 * (JDBC Socket Factory). Activa solo bajo el perfil {@code gcp}; sin ese perfil, Spring Boot sigue
 * autoconfigurando H2 como hasta ahora.
 *
 * @since 1.0
 */
@Configuration
@Profile("gcp")
public class CloudSqlDataSourceConfig {

  /** Usuario de la base de datos ({@code hogwarts.datasource.cloud-sql.username}). */
  @Value("${hogwarts.datasource.cloud-sql.username}")
  private String username;

  /** Password del usuario ({@code hogwarts.datasource.cloud-sql.password}). */
  @Value("${hogwarts.datasource.cloud-sql.password}")
  private String password;

  /** Nombre de la base de datos ({@code hogwarts.datasource.cloud-sql.db-name}). */
  @Value("${hogwarts.datasource.cloud-sql.db-name}")
  private String dbName;

  /**
   * Nombre de conexión de la instancia de Cloud SQL, formato {@code PROJECT_ID:REGION:INSTANCE_ID}
   * ({@code hogwarts.datasource.cloud-sql.instance-connection-name}).
   */
  @Value("${hogwarts.datasource.cloud-sql.instance-connection-name}")
  private String instanceConnectionName;

  /**
   * Crea el {@link DataSource} que se conecta a Cloud SQL mediante el socket factory de PostgreSQL,
   * usando los valores leídos de las properties de este perfil.
   *
   * @return un {@link HikariDataSource} configurado con el Cloud SQL Java Connector
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
