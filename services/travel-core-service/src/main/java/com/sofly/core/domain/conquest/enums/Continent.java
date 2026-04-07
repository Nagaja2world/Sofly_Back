package com.sofly.core.domain.conquest.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Continent {
    ASIA("아시아"),
    EUROPE("유럽"),
    NORTH_AMERICA("북아메리카"),
    SOUTH_AMERICA("남아메리카"),
    AFRICA("아프리카"),
    OCEANIA("오세아니아");

    private final String displayName;
}
