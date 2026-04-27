package com.sofly.core.domain.conquest.repository;

import com.sofly.core.domain.conquest.entity.VisitedCity;
import com.sofly.core.domain.conquest.enums.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitedCityRepository extends JpaRepository<VisitedCity, Long> {

    List<VisitedCity> findByUserId(Long userId);

    Optional<VisitedCity> findByUserIdAndCityNameAndCountryCode(Long userId, String cityName, String countryCode);

    List<VisitedCity> findByUserIdAndCountryCode(Long userId, String countryCode);

    List<VisitedCity> findByStatus(VisitStatus status);

    List<VisitedCity> findByUserIdAndCountryCodeAndStatus(Long userId, String countryCode, VisitStatus status);
}
