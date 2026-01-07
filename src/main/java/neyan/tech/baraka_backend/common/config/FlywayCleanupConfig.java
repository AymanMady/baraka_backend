package neyan.tech.baraka_backend.common.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration pour nettoyer automatiquement la base de données au démarrage
 * en mode développement avec Docker.
 * 
 * Activez cette fonctionnalité en définissant FLYWAY_CLEAN_ON_STARTUP=true
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.flyway.clean-on-startup", havingValue = "true")
@Profile("dev")
public class FlywayCleanupConfig {

    @Autowired
    private Flyway flyway;

    @Bean
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            log.warn("⚠️  FLYWAY_CLEAN_ON_STARTUP is enabled - Database will be cleaned before migrations!");
            log.info("🧹 Cleaning database...");
            flyway.clean();
            log.info("✅ Database cleaned successfully");
            log.info("🔄 Running migrations...");
            flyway.migrate();
            log.info("✅ Migrations completed successfully");
        };
    }
}

