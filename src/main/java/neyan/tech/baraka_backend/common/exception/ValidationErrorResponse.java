package neyan.tech.baraka_backend.common.exception;

import java.time.Instant;
import java.util.Map;

public record ValidationErrorResponse(
        int status,
        String message,
        Map<String, String> errors,
        Instant timestamp
) {
}

