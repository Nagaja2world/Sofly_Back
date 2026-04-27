package com.sofly.core.domain.conquest.service;

import com.sofly.core.domain.conquest.dto.request.BulkImportRequest;
import com.sofly.core.domain.conquest.dto.request.CityCreateRequest;
import com.sofly.core.domain.conquest.dto.request.StatusUpdateRequest;
import com.sofly.core.domain.conquest.dto.response.*;
import com.sofly.core.domain.conquest.entity.VisitedCity;
import com.sofly.core.domain.conquest.entity.VisitedCountry;
import com.sofly.core.domain.conquest.enums.Continent;
import com.sofly.core.domain.conquest.enums.VisitStatus;
import com.sofly.core.domain.conquest.repository.VisitedCityRepository;
import com.sofly.core.domain.conquest.repository.VisitedCountryRepository;
import com.sofly.core.domain.conquest.service.AirportInfoService.AirportInfo;
import com.sofly.core.domain.conquest.code.ConquestErrorCode;
import com.sofly.core.domain.conquest.exception.ConquestException;
import com.sofly.core.domain.user.entity.User;
import com.sofly.core.domain.user.repository.UserRepository;
import com.sofly.core.domain.workspace.code.WorkspaceErrorCode;
import com.sofly.core.domain.workspace.entity.SavedFlight;
import com.sofly.core.domain.workspace.entity.Workspace;
import com.sofly.core.domain.workspace.exception.WorkspaceException;
import com.sofly.core.domain.workspace.repository.SavedFlightRepository;
import com.sofly.core.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConquestMapService {

    private final VisitedCountryRepository visitedCountryRepository;
    private final VisitedCityRepository visitedCityRepository;
    private final AirportInfoService airportInfoService;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final SavedFlightRepository savedFlightRepository;

    // ── 정복 지도 전체 조회 ─────────────────────────────────────────────

    public ConquestMapResponse getConquestMap(Long userId) {
        List<VisitedCountryResponse> countries = visitedCountryRepository.findByUserId(userId).stream()
                .map(VisitedCountryResponse::from)
                .toList();
        List<VisitedCityResponse> cities = visitedCityRepository.findByUserId(userId).stream()
                .map(VisitedCityResponse::from)
                .toList();
        return ConquestMapResponse.builder()
                .countries(countries)
                .cities(cities)
                .build();
    }

    // ── 통계 패널 ───────────────────────────────────────────────────────

    public ConquestStatsResponse getStats(Long userId) {
        List<VisitedCountry> visitedCountries = visitedCountryRepository
                .findByUserIdAndStatus(userId, VisitStatus.VISITED);
        List<VisitedCity> visitedCities = visitedCityRepository
                .findByUserId(userId).stream()
                .filter(c -> c.getStatus() == VisitStatus.VISITED)
                .toList();

        int totalTravelDays = calculateTotalTravelDays(userId);
        double totalDistanceKm = calculateTotalDistanceKm(userId);

        Map<Continent, Long> continentCountMap = visitedCountries.stream()
                .collect(Collectors.groupingBy(VisitedCountry::getContinent, Collectors.counting()));

        List<ConquestStatsResponse.ContinentStats> continentStats = Arrays.stream(Continent.values())
                .map(continent -> ConquestStatsResponse.ContinentStats.builder()
                        .continent(continent)
                        .continentName(continent.getDisplayName())
                        .visitedCountryCount(continentCountMap.getOrDefault(continent, 0L).intValue())
                        .build())
                .toList();

        return ConquestStatsResponse.of(
                visitedCountries.size(),
                visitedCities.size(),
                totalTravelDays,
                Math.round(totalDistanceKm * 10.0) / 10.0,
                continentStats
        );
    }

    // ── 국가 상태 수동 변경 ─────────────────────────────────────────────

    @Transactional
    public VisitedCountryResponse updateCountryStatus(Long userId, String countryCode, StatusUpdateRequest request) {
        VisitedCountry visitedCountry = visitedCountryRepository
                .findByUserIdAndCountryCode(userId, countryCode)
                .orElseGet(() -> createVisitedCountry(userId, countryCode));

        visitedCountry.updateStatus(request.getStatus());
        return VisitedCountryResponse.from(visitedCountry);
    }

    // ── 도시 수동 추가 ──────────────────────────────────────────────────

    @Transactional
    public VisitedCityResponse addOrUpdateCity(Long userId, CityCreateRequest request) {
        User user = userRepository.getReferenceById(userId);
        VisitedCity visitedCity = visitedCityRepository
                .findByUserIdAndCityNameAndCountryCode(userId, request.getCityName(), request.getCountryCode())
                .orElseGet(() -> visitedCityRepository.save(VisitedCity.builder()
                        .user(user)
                        .cityName(request.getCityName())
                        .countryCode(request.getCountryCode())
                        .latitude(request.getLatitude())
                        .longitude(request.getLongitude())
                        .build()));

        visitedCity.updateStatus(request.getStatus());
        return VisitedCityResponse.from(visitedCity);
    }

    // ── 도시 상태 변경 ──────────────────────────────────────────────────

    @Transactional
    public VisitedCityResponse updateCityStatus(Long userId, Long cityId, StatusUpdateRequest request) {
        VisitedCity visitedCity = visitedCityRepository.findById(cityId)
                .orElseThrow(() -> new ConquestException(ConquestErrorCode.CITY_NOT_FOUND));
        if (!visitedCity.getUser().getId().equals(userId)) {
            throw new ConquestException(ConquestErrorCode.CONQUEST_FORBIDDEN);
        }
        visitedCity.updateStatus(request.getStatus());
        return VisitedCityResponse.from(visitedCity);
    }

    // ── 과거 방문 일괄 등록 ─────────────────────────────────────────────

    @Transactional
    public void bulkImport(Long userId, BulkImportRequest request) {
        User user = userRepository.getReferenceById(userId);

        if (request.getCountries() != null) {
            for (BulkImportRequest.CountryImport ci : request.getCountries()) {
                VisitedCountry country = visitedCountryRepository
                        .findByUserIdAndCountryCode(userId, ci.getCountryCode())
                        .orElseGet(() -> {
                            Continent continent = airportInfoService.getContinentByCountryCode(ci.getCountryCode());
                            return visitedCountryRepository.save(VisitedCountry.builder()
                                    .user(user)
                                    .countryCode(ci.getCountryCode())
                                    .countryName(ci.getCountryCode()) // 국가명 미입력 시 코드로 저장
                                    .continent(continent)
                                    .build());
                        });
                country.updateStatus(ci.getStatus());
            }
        }

        if (request.getCities() != null) {
            for (BulkImportRequest.CityImport ci : request.getCities()) {
                VisitedCity city = visitedCityRepository
                        .findByUserIdAndCityNameAndCountryCode(userId, ci.getCityName(), ci.getCountryCode())
                        .orElseGet(() -> visitedCityRepository.save(VisitedCity.builder()
                                .user(user)
                                .cityName(ci.getCityName())
                                .countryCode(ci.getCountryCode())
                                .latitude(ci.getLatitude())
                                .longitude(ci.getLongitude())
                                .build()));
                city.updateStatus(ci.getStatus());
            }
        }
    }

    // ── 국가별 연관 워크스페이스 조회 ──────────────────────────────────

    public List<WorkspaceConquestResponse> getWorkspacesByCountry(Long userId, String countryCode) {
        return workspaceRepository.findAllByUserIdAndCountryCode(userId, countryCode).stream()
                .map(WorkspaceConquestResponse::from)
                .toList();
    }

    // ── 여행 경로(Arc) 조회 ─────────────────────────────────────────────

    public List<TripRouteResponse> getTripRoutes(Long userId) {
        List<Workspace> workspaces = workspaceRepository.findAllWithOwnerByUserId(userId);
        List<TripRouteResponse> routes = new ArrayList<>();

        for (Workspace workspace : workspaces) {
            List<SavedFlight> flights = savedFlightRepository.findAllByWorkspaceId(workspace.getId());
            for (SavedFlight flight : flights) {
                buildRoute(flight, workspace).ifPresent(routes::add);
            }
        }
        return routes;
    }

    public List<TripRouteResponse> getTripRoutesByWorkspace(Long userId, Long workspaceId) {
        Workspace workspace = workspaceRepository.findWithOwnerById(workspaceId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
        return savedFlightRepository.findAllByWorkspaceId(workspaceId).stream()
                .map(flight -> buildRoute(flight, workspace))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    @Transactional
    public void promoteToVisited(Long userId, String countryCode) {
        // 국가 VISITED 전환 (이미 VISITED이면 스킵 → 멱등성 보장)
        visitedCountryRepository.findByUserIdAndCountryCode(userId, countryCode)
                .ifPresent(vc -> {
                    if (vc.getStatus() == VisitStatus.PLANNED) {
                        vc.updateStatus(VisitStatus.VISITED);
                        log.info("국가 PLANNED→VISITED: userId={}, country={}", userId, countryCode);
                    }
                });

        // 해당 국가의 도시들 VISITED 전환
        visitedCityRepository.findByUserIdAndCountryCodeAndStatus(userId, countryCode, VisitStatus.PLANNED)
                .forEach(city -> {
                    city.updateStatus(VisitStatus.VISITED);
                    log.info("도시 PLANNED→VISITED: userId={}, city={}", userId, city.getCityName());
                });
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────────

    private VisitedCountry createVisitedCountry(Long userId, String countryCode) {
        User user = userRepository.getReferenceById(userId);
        AirportInfo info = airportInfoService.findByIata(countryCode).orElse(null);
        Continent continent = airportInfoService.getContinentByCountryCode(countryCode);
        String countryName = countryCode; // 기본값, 공항 정보로 덮어쓰기
        return visitedCountryRepository.save(VisitedCountry.builder()
                .user(user)
                .countryCode(countryCode)
                .countryName(countryName)
                .continent(continent)
                .build());
    }

    @Transactional
    public void applyPlannedStatus(User user, AirportInfo arrivalInfo) {
        // 국가 PLANNED 처리
        visitedCountryRepository
                .findByUserIdAndCountryCode(user.getId(), arrivalInfo.countryCode())
                .ifPresentOrElse(
                        vc -> {
                            if (vc.getStatus() == VisitStatus.UNVISITED) {
                                vc.updateStatus(VisitStatus.PLANNED);
                            }
                        },
                        () -> visitedCountryRepository.save(VisitedCountry.builder()
                                .user(user)
                                .countryCode(arrivalInfo.countryCode())
                                .countryName(arrivalInfo.countryNameKo())
                                .continent(arrivalInfo.continent())
                                .status(VisitStatus.PLANNED)
                                .build())
                );

        // 도시 PLANNED 처리
        visitedCityRepository
                .findByUserIdAndCityNameAndCountryCode(user.getId(), arrivalInfo.cityName(), arrivalInfo.countryCode())
                .ifPresentOrElse(
                        vc -> {
                            if (vc.getStatus() == VisitStatus.UNVISITED) {
                                vc.updateStatus(VisitStatus.PLANNED);
                            }
                        },
                        () -> visitedCityRepository.save(VisitedCity.builder()
                                .user(user)
                                .cityName(arrivalInfo.cityName())
                                .countryCode(arrivalInfo.countryCode())
                                .latitude(arrivalInfo.latitude())
                                .longitude(arrivalInfo.longitude())
                                .status(VisitStatus.PLANNED)
                                .build())
                );
    }

    private boolean hasDepartedFlightToCountry(Long userId, String countryCode, LocalDateTime now) {
        List<Workspace> workspaces = workspaceRepository.findAllWithOwnerByUserId(userId);
        for (Workspace workspace : workspaces) {
            List<SavedFlight> flights = savedFlightRepository.findAllByWorkspaceId(workspace.getId());
            for (SavedFlight flight : flights) {
                if (flight.getDepartureTime().isBefore(now)) {
                    AirportInfo arrivalInfo = airportInfoService.findByIata(flight.getArrivalAirport()).orElse(null);
                    if (arrivalInfo != null && arrivalInfo.countryCode().equals(countryCode)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Optional<TripRouteResponse> buildRoute(SavedFlight flight, Workspace workspace) {
        AirportInfo depInfo = airportInfoService.findByIata(flight.getDepartureAirport()).orElse(null);
        AirportInfo arrInfo = airportInfoService.findByIata(flight.getArrivalAirport()).orElse(null);
        if (depInfo == null || arrInfo == null) return Optional.empty();

        double distanceKm = airportInfoService.calculateDistanceKm(
                depInfo.latitude(), depInfo.longitude(),
                arrInfo.latitude(), arrInfo.longitude()
        );
        TripRouteResponse.RouteType routeType = depInfo.countryCode().equals(arrInfo.countryCode())
                ? TripRouteResponse.RouteType.DOMESTIC
                : TripRouteResponse.RouteType.INTERNATIONAL;

        return Optional.of(TripRouteResponse.builder()
                .flightId(flight.getId())
                .workspaceId(workspace.getId())
                .workspaceTitle(workspace.getTitle())
                .departureAirport(flight.getDepartureAirport())
                .departureCity(depInfo.cityName())
                .departureCountryCode(depInfo.countryCode())
                .departureLat(depInfo.latitude())
                .departureLng(depInfo.longitude())
                .arrivalAirport(flight.getArrivalAirport())
                .arrivalCity(arrInfo.cityName())
                .arrivalCountryCode(arrInfo.countryCode())
                .arrivalLat(arrInfo.latitude())
                .arrivalLng(arrInfo.longitude())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .airline(flight.getAirline())
                .flightNumber(flight.getFlightNumber())
                .distanceKm(Math.round(distanceKm * 10.0) / 10.0)
                .routeType(routeType)
                .build());
    }

    private int calculateTotalTravelDays(Long userId) {
        return workspaceRepository.findAllWithOwnerByUserId(userId).stream()
                .filter(ws -> ws.getEndDate() != null && ws.getStartDate() != null
                        && !ws.getEndDate().isAfter(LocalDate.now()))
                .mapToInt(ws -> (int) (ws.getEndDate().toEpochDay() - ws.getStartDate().toEpochDay() + 1))
                .sum();
    }

    private double calculateTotalDistanceKm(Long userId) {
        return workspaceRepository.findAllWithOwnerByUserId(userId).stream()
                .flatMap(ws -> savedFlightRepository.findAllByWorkspaceId(ws.getId()).stream())
                .filter(flight -> flight.getDepartureTime().isBefore(LocalDateTime.now()))
                .mapToDouble(flight -> {
                    AirportInfo dep = airportInfoService.findByIata(flight.getDepartureAirport()).orElse(null);
                    AirportInfo arr = airportInfoService.findByIata(flight.getArrivalAirport()).orElse(null);
                    if (dep == null || arr == null) return 0;
                    return airportInfoService.calculateDistanceKm(
                            dep.latitude(), dep.longitude(), arr.latitude(), arr.longitude());
                })
                .sum();
    }
}
