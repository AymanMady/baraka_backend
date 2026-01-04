package neyan.tech.baraka_backend.common.geo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper for an entity with its distance from a reference point.
 *
 * @param <T> The entity type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoResult<T> {

    private T item;
    private double distanceKm;

    public static <T> GeoResult<T> of(T item, double distanceKm) {
        return new GeoResult<>(item, distanceKm);
    }
}

