package neyan.tech.baraka_backend.common.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration pour nettoyer automatiquement la base de données au démarrage.
 * 
 * ⚠️ ATTENTION : Cette fonctionnalité nettoie complètement la base de données !
 * Utilisez-la uniquement en développement, jamais en production !
 * 
 * Activez cette fonctionnalité en définissant FLYWAY_CLEAN_ON_STARTUP=true
 * et en vous assurant que spring.flyway.clean-disabled=false
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.flyway.clean-on-startup", havingValue = "true")
public class FlywayCleanupConfig {

    @Autowired
    private Flyway flyway;
    
    @Autowired
    private FlywayProperties flywayProperties;

    @Bean
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            // Vérifier que le nettoyage n'est pas désactivé
            if (flywayProperties.isCleanDisabled()) {
                log.warn("⚠️  FLYWAY_CLEAN_ON_STARTUP is enabled but clean is disabled in configuration!");
                log.warn("⚠️  Skipping database cleanup. Set spring.flyway.clean-disabled=false to enable cleaning");
                log.info("🔄 Running migrations without cleaning...");
                flyway.migrate();
                log.info("✅ Migrations completed successfully");
                return;
            }
            
            log.warn("⚠️  ⚠️  ⚠️  FLYWAY_CLEAN_ON_STARTUP is enabled - Database will be cleaned before migrations! ⚠️  ⚠️  ⚠️");
            log.info("🧹 Cleaning database...");
            flyway.clean();
            log.info("✅ Database cleaned successfully");
            log.info("🔄 Running migrations...");
            flyway.migrate();
            log.info("✅ Migrations completed successfully");
        };
    }
}

