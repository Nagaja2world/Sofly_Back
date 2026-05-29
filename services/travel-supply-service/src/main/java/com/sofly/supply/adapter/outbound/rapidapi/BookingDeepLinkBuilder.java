package com.sofly.supply.adapter.outbound.rapidapi;

import org.springframework.web.util.UriComponentsBuilder;

public class BookingDeepLinkBuilder {

    private BookingDeepLinkBuilder() {}

    /**
     * Booking.com 항공권 검색 딥링크 생성.
     * <p>RapidAPI의 flightOffers[].token은 getFlightDetails API용 토큰이며,
     * flights.booking.com 웹 경로의 offer token과 항상 호환된다고 볼 수 없다.
     */
    public static String buildFlightSearchUrl(String origin, String dest,
                                              String departDate, String returnDate,
                                              String tripType, String cabinClass, int adults) {
        String route = origin + ".AIRPORT-" + dest + ".AIRPORT";
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://flights.booking.com/flights/" + route + "/")
                .queryParam("type", tripType)
                .queryParam("adults", adults)
                .queryParam("cabinClass", cabinClass != null ? cabinClass : "ECONOMY")
                .queryParam("from", origin + ".AIRPORT")
                .queryParam("to", dest + ".AIRPORT")
                .queryParam("depart", departDate)
                .queryParam("sort", "BEST");
        if (returnDate != null && !returnDate.isBlank()) {
            builder.queryParam("return", returnDate);
        }
        return builder.build().toUriString();
    }

    /**
     * @deprecated Use {@link #buildFlightSearchUrl(String, String, String, String, String, String, int)}.
     * The token accepted here is an API token, not a reliable public Booking.com URL token.
     */
    @Deprecated
    public static String buildFlightUrl(String token, String origin, String dest,
                                  String departDate, String returnDate,
                                  String tripType, String cabinClass, int adults) {
        return buildFlightSearchUrl(origin, dest, departDate, returnDate, tripType, cabinClass, adults);
    }

    /**
     * Booking.com 호텔 예약 딥링크 생성
     * <p>예) https://www.booking.com/hotel/kr/half-rest-hostel-jongno-insa.html
     * ?checkin=2026-05-29&checkout=2026-05-30&group_adults=2
     * <p>API 응답에 slug가 없으므로 property.name을 slugify하여 사용.
     * slug 불일치 시 Booking.com이 검색 결과 페이지로 fallback 리다이렉트함.
     *
     * @param hotelName   property.name (예: Half Rest Hostel Jongno)
     * @param countryCode property.countryCode (예: kr)
     * @param checkin     체크인 날짜 (yyyy-MM-dd)
     * @param checkout    체크아웃 날짜 (yyyy-MM-dd)
     * @param adults      성인 수
     */
    public static String buildHotelUrl(String hotelName, String countryCode,
                                 String checkin, String checkout, int adults) {
        String slug = slugify(hotelName);
        return UriComponentsBuilder
                .fromHttpUrl("https://www.booking.com/hotel/" + countryCode + "/" + slug + ".html")
                .queryParam("checkin", checkin)
                .queryParam("checkout", checkout)
                .queryParam("group_adults", adults)
                .build()
                .toUriString();
    }

    /** "The Taj Mahal Tower, Mumbai" → "the-taj-mahal-tower-mumbai" */
    private static String slugify(String name) {
        if (name == null) return "hotel";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }
}
