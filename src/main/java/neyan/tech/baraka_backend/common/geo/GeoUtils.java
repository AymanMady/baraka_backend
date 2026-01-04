package neyan.tech.baraka_backend.common.geo;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;

/**
 * Utility class for geographic calculations.
 */
@UtilityClass
public class GeoUtils {

    /**
     * Earth's radius in kilometers
     */
    public static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Degrees per kilometer (approximate, at equator)
     */
    public static final double DEGREES_PER_KM_LAT = 1.0 / 111.0;

    /**
     * Calculates the distance between two points using the Haversine formula.
     *
     * @param lat1 Latitude of point 1 in degrees
     * @param lng1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lng2 Longitude of point 2 in degrees
     * @return Distance in kilometers
     */
    public static double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Overload for BigDecimal coordinates.
     */
    public static double haversineDistance(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return Double.MAX_VALUE;
        }
        return haversineDistance(
                lat1.doubleValue(), lng1.doubleValue(),
                lat2.doubleValue(), lng2.doubleValue()
        );
    }

    /**
     * Calculates a bounding box around a point.
     *
     * @param lat      Center latitude
     * @param lng      Center longitude
     * @param radiusKm Radius in kilometers
     * @return BoundingBox with min/max lat/lng
     */
    public static BoundingBox calculateBoundingBox(double lat, double lng, double radiusKm) {
        // Latitude: 1 degree ≈ 111 km
        double latDelta = radiusKm * DEGREES_PER_KM_LAT;

        // Longitude: varies by latitude (cos factor)
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        return new BoundingBox(
                lat - latDelta,  // minLat
                lat + latDelta,  // maxLat
                lng - lngDelta,  // minLng
                lng + lngDelta   // maxLng
        );
    }

    /**
     * Checks if a point is within a bounding box.
     */
    public static boolean isWithinBoundingBox(double lat, double lng, BoundingBox box) {
        return lat >= box.minLat() && lat <= box.maxLat()
                && lng >= box.minLng() && lng <= box.maxLng();
    }

    /**
     * Checks if coordinates are valid.
     */
    public static boolean isValidCoordinate(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return false;
        }
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    /**
     * Checks if BigDecimal coordinates are valid.
     */
    public static boolean isValidCoordinate(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            return false;
        }
        return isValidCoordinate(lat.doubleValue(), lng.doubleValue());
    }

    /**
     * Bounding box record for geographic queries.
     */
    public record BoundingBox(
            double minLat,
            double maxLat,
            double minLng,
            double maxLng
    ) {
        public BigDecimal minLatDecimal() {
            return BigDecimal.valueOf(minLat);
        }

        public BigDecimal maxLatDecimal() {
            return BigDecimal.valueOf(maxLat);
        }

        public BigDecimal minLngDecimal() {
            return BigDecimal.valueOf(minLng);
        }

        public BigDecimal maxLngDecimal() {
            return BigDecimal.valueOf(maxLng);
        }
    }
}

