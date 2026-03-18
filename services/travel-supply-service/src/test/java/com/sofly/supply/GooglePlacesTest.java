package com.sofly.supply;

import com.sofly.supply.adapter.outbound.google.GooglePlacesClient;
import com.sofly.supply.adapter.outbound.google.PlaceInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GooglePlacesTest {
    @Autowired
    private GooglePlacesClient googlePlacesClient;

    @Test
    void testGooglePlace() {
        PlaceInfo info = googlePlacesClient.fetchPlaceInfo("Hyatt Regency Paris Etoile", "Paris");
        System.out.println("result = " + info);
    }
}
