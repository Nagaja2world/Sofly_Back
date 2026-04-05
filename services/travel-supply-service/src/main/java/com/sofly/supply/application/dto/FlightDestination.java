package com.sofly.supply.application.dto;

public record FlightDestination(
        String id,
        String type,
        String name,
        String code,
        String city,
        String cityName,
        String regionName,
        String country,
        String countryName,
        String countryNameShort,
        String photoUrl,
        DistanceToCity distanceToCity,
        String parent
){
    public record DistanceToCity(
      double value,
      String unit
    ){}
}
