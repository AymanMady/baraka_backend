package neyan.tech.baraka_backend.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

@Configuration
public class JacksonConfig {

    /**
     * Custom deserializer for Instant that accepts dates with or without timezone.
     * Handles formats like:
     * - "2026-01-09T19:51:00.000" (without timezone - treated as UTC)
     * - "2026-01-09T19:51:00.000Z" (with Z timezone)
     * - "2026-01-09T19:51:00.000+00:00" (with offset)
     */
    private static class LenientInstantDeserializer extends StdDeserializer<Instant> {
        
        private static final DateTimeFormatter LENIENT_FORMATTER = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .optionalStart()
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .optionalEnd()
                .toFormatter();

        public LenientInstantDeserializer() {
            super(Instant.class);
        }

        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateString = p.getText();
            
            if (dateString == null || dateString.trim().isEmpty()) {
                return null;
            }
            
            // Try parsing as ISO-8601 with timezone first (default behavior)
            // This handles formats like: "2026-01-09T19:51:00.000Z" or "2026-01-09T19:51:00.000+00:00"
            try {
                return Instant.parse(dateString);
            } catch (Exception e) {
                // If parsing fails, try parsing without timezone and treat as UTC
                // This handles formats like: "2026-01-09T19:51:00.000"
                try {
                    // Remove timezone if present at the end (Z, +HH:MM, -HH:MM, etc.)
                    String cleaned = dateString.trim();
                    // Remove 'Z' at the end
                    if (cleaned.endsWith("Z")) {
                        cleaned = cleaned.substring(0, cleaned.length() - 1);
                    }
                    // Remove timezone offset like +00:00, -05:00, etc.
                    cleaned = cleaned.replaceAll("[+-]\\d{2}:\\d{2}$", "");
                    
                    // Parse as LocalDateTime and convert to Instant at UTC
                    LocalDateTime localDateTime = LocalDateTime.parse(cleaned, LENIENT_FORMATTER);
                    return localDateTime.atZone(ZoneOffset.UTC).toInstant();
                } catch (Exception e2) {
                    throw new IOException("Cannot deserialize value of type `java.time.Instant` from String \"" + dateString + "\". " +
                            "Expected format: ISO-8601 with optional timezone (e.g., '2026-01-09T19:51:00.000' or '2026-01-09T19:51:00.000Z')", e2);
                }
            }
        }
    }

    /**
     * Customize Jackson ObjectMapper to use lenient Instant deserializer.
     * This approach works with Spring Boot's auto-configuration.
     * Our custom deserializer will override the default one from JavaTimeModule.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            // Create a SimpleModule with our lenient Instant deserializer
            // This will override the default Instant deserializer from JavaTimeModule
            SimpleModule customModule = new SimpleModule("LenientInstantModule");
            customModule.addDeserializer(Instant.class, new LenientInstantDeserializer());
            
            // Add the custom module after the default modules are installed
            // Our deserializer will take precedence over the default one
            builder.modulesToInstall(customModule);
            
            // Configure additional features
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        };
    }
}

