package com.sofly.supply.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sofly.supply.application.dto.HotelDestination;
import com.sofly.supply.application.dto.HotelOptionsRequest;
import com.sofly.supply.application.dto.HotelSearchRequest;
import com.sofly.supply.application.dto.HotelSortOption;
import com.sofly.supply.application.port.outbound.HotelMetaPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotelSearchService {

    private final SupplierRouter router;
    private final HotelMetaPort hotelMetaPort;

    public HotelSearchService(SupplierRouter router, HotelMetaPort hotelMetaPort) {
        this.router = router;
        this.hotelMetaPort = hotelMetaPort;
    }

    public JsonNode search(String supplier, HotelSearchRequest request) {
        return router.selectHotelSupplier(supplier).searchHotelsByCity(request);
    }

    public List<HotelDestination> searchDestination(String query) {
        return hotelMetaPort.searchDestination(query);
    }

    public List<HotelSortOption> getSortBy(HotelOptionsRequest request) {
        return hotelMetaPort.getSortBy(request);
    }

    public JsonNode getFilter(HotelOptionsRequest request) {
        return hotelMetaPort.getFilter(request);
    }
}
