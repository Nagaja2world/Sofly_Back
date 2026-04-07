package com.sofly.core.domain.conquest.repository;

import com.sofly.core.domain.conquest.entity.VisitedCountry;
import com.sofly.core.domain.conquest.enums.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitedCountryRepository extends JpaRepository<VisitedCountry, Long> {

    List<VisitedCountry> findByUserId(Long userId);

    Optional<VisitedCountry> findByUserIdAndCountryCode(Long userId, String countryCode);

    List<VisitedCountry> findByUserIdAndStatus(Long userId, VisitStatus status);

    List<VisitedCountry> findByStatus(VisitStatus status);
}
