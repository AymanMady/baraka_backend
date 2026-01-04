package neyan.tech.baraka_backend.common.geo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import neyan.tech.baraka_backend.common.geo.GeoService.BasketWithDistance;
import neyan.tech.baraka_backend.common.geo.GeoService.ShopWithDistance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nearby")
@RequiredArgsConstructor
@Tag(name = "Nearby Search", description = "Geographic search endpoints")
public class GeoController {

    private final GeoService geoService;

    @Operation(summary = "Find nearby shops", 
               description = "Returns shops sorted by distance from the specified location")
    @GetMapping("/shops")
    public ResponseEntity<Page<ShopWithDistance>> findNearbyShops(
            @Parameter(description = "Latitude", required = true, example = "6.3703")
            @RequestParam Double lat,
            @Parameter(description = "Longitude", required = true, example = "2.3912")
            @RequestParam Double lng,
            @Parameter(description = "Search radius in kilometers", example = "10")
            @RequestParam(defaultValue = "10") Double radiusKm,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(geoService.findNearbyShops(lat, lng, radiusKm, pageable));
    }

    @Operation(summary = "Find nearby baskets", 
               description = "Returns available baskets sorted by distance from the specified location")
    @GetMapping("/baskets")
    public ResponseEntity<Page<BasketWithDistance>> findNearbyBaskets(
            @Parameter(description = "Latitude", required = true, example = "6.3703")
            @RequestParam Double lat,
            @Parameter(description = "Longitude", required = true, example = "2.3912")
            @RequestParam Double lng,
            @Parameter(description = "Search radius in kilometers", example = "10")
            @RequestParam(defaultValue = "10") Double radiusKm,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(geoService.findNearbyBaskets(lat, lng, radiusKm, pageable));
    }

    @Operation(summary = "Find shops with available baskets", 
               description = "Returns shops that have available baskets, sorted by distance")
    @GetMapping("/shops-with-baskets")
    public ResponseEntity<Page<ShopWithDistance>> findShopsWithBaskets(
            @Parameter(description = "Latitude", required = true, example = "6.3703")
            @RequestParam Double lat,
            @Parameter(description = "Longitude", required = true, example = "2.3912")
            @RequestParam Double lng,
            @Parameter(description = "Search radius in kilometers", example = "10")
            @RequestParam(defaultValue = "10") Double radiusKm,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(geoService.findShopsWithAvailableBaskets(lat, lng, radiusKm, pageable));
    }
}

