package com.sofly.core.domain.conquest.service;

import com.sofly.core.domain.conquest.enums.Continent;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class AirportInfoService {

    public record AirportInfo(
            String iataCode,
            String countryCode,
            String countryNameKo,
            String cityName,
            double latitude,
            double longitude,
            Continent continent
    ) {}

    private static final Map<String, AirportInfo> AIRPORT_MAP = Map.ofEntries(
            // ── 한국 ──────────────────────────────────────────────────────────
            entry("ICN", "KR", "대한민국", "인천", 37.4602, 126.4407, Continent.ASIA),
            entry("GMP", "KR", "대한민국", "서울", 37.5583, 126.7906, Continent.ASIA),
            entry("PUS", "KR", "대한민국", "부산", 35.1795, 128.9384, Continent.ASIA),
            entry("CJU", "KR", "대한민국", "제주", 33.5113, 126.4930, Continent.ASIA),
            // ── 일본 ──────────────────────────────────────────────────────────
            entry("NRT", "JP", "일본", "도쿄", 35.7647, 140.3864, Continent.ASIA),
            entry("HND", "JP", "일본", "도쿄", 35.5494, 139.7798, Continent.ASIA),
            entry("KIX", "JP", "일본", "오사카", 34.4272, 135.2440, Continent.ASIA),
            entry("ITM", "JP", "일본", "오사카", 34.7855, 135.4380, Continent.ASIA),
            entry("NGO", "JP", "일본", "나고야", 34.8583, 136.8048, Continent.ASIA),
            entry("FUK", "JP", "일본", "후쿠오카", 33.5858, 130.4508, Continent.ASIA),
            entry("CTS", "JP", "일본", "삿포로", 42.7752, 141.6922, Continent.ASIA),
            entry("OKA", "JP", "일본", "오키나와", 26.1958, 127.6464, Continent.ASIA),
            // ── 중국 ──────────────────────────────────────────────────────────
            entry("PEK", "CN", "중국", "베이징", 40.0799, 116.6031, Continent.ASIA),
            entry("PKX", "CN", "중국", "베이징", 39.5098, 116.4105, Continent.ASIA),
            entry("PVG", "CN", "중국", "상하이", 31.1443, 121.8083, Continent.ASIA),
            entry("SHA", "CN", "중국", "상하이", 31.1983, 121.3363, Continent.ASIA),
            entry("CAN", "CN", "중국", "광저우", 23.3924, 113.2990, Continent.ASIA),
            entry("SZX", "CN", "중국", "선전", 22.6390, 113.8103, Continent.ASIA),
            entry("CTU", "CN", "중국", "청두", 30.5784, 103.9469, Continent.ASIA),
            entry("CKG", "CN", "중국", "충칭", 29.7192, 106.6417, Continent.ASIA),
            // ── 대만·홍콩 ─────────────────────────────────────────────────────
            entry("TPE", "TW", "대만", "타이베이", 25.0777, 121.2328, Continent.ASIA),
            entry("HKG", "HK", "홍콩", "홍콩", 22.3080, 113.9185, Continent.ASIA),
            // ── 동남아시아 ────────────────────────────────────────────────────
            entry("BKK", "TH", "태국", "방콕", 13.6811, 100.7474, Continent.ASIA),
            entry("DMK", "TH", "태국", "방콕", 13.9126, 100.6069, Continent.ASIA),
            entry("HKT", "TH", "태국", "푸껫", 8.1132, 98.3169, Continent.ASIA),
            entry("CNX", "TH", "태국", "치앙마이", 18.7668, 98.9627, Continent.ASIA),
            entry("SGN", "VN", "베트남", "호치민", 10.8188, 106.6520, Continent.ASIA),
            entry("HAN", "VN", "베트남", "하노이", 21.2212, 105.8072, Continent.ASIA),
            entry("DAD", "VN", "베트남", "다낭", 16.0439, 108.1992, Continent.ASIA),
            entry("SIN", "SG", "싱가포르", "싱가포르", 1.3644, 103.9915, Continent.ASIA),
            entry("KUL", "MY", "말레이시아", "쿠알라룸푸르", 2.7456, 101.7072, Continent.ASIA),
            entry("MNL", "PH", "필리핀", "마닐라", 14.5086, 121.0194, Continent.ASIA),
            entry("CGK", "ID", "인도네시아", "자카르타", -6.1256, 106.6559, Continent.ASIA),
            entry("DPS", "ID", "인도네시아", "발리", -8.7482, 115.1670, Continent.ASIA),
            entry("RGN", "MM", "미얀마", "양곤", 16.9073, 96.1332, Continent.ASIA),
            entry("REP", "KH", "캄보디아", "씨엠립", 13.4107, 103.8129, Continent.ASIA),
            entry("VTE", "LA", "라오스", "비엔티안", 17.9883, 102.5633, Continent.ASIA),
            // ── 남아시아 ──────────────────────────────────────────────────────
            entry("BOM", "IN", "인도", "뭄바이", 19.0896, 72.8656, Continent.ASIA),
            entry("DEL", "IN", "인도", "델리", 28.5562, 77.0999, Continent.ASIA),
            entry("BLR", "IN", "인도", "벵갈루루", 13.1986, 77.7066, Continent.ASIA),
            entry("CMB", "LK", "스리랑카", "콜롬보", 7.1808, 79.8841, Continent.ASIA),
            entry("KTM", "NP", "네팔", "카트만두", 27.6966, 85.3591, Continent.ASIA),
            // ── 중동 ──────────────────────────────────────────────────────────
            entry("DXB", "AE", "아랍에미리트", "두바이", 25.2532, 55.3657, Continent.ASIA),
            entry("AUH", "AE", "아랍에미리트", "아부다비", 24.4330, 54.6511, Continent.ASIA),
            entry("DOH", "QA", "카타르", "도하", 25.2609, 51.6138, Continent.ASIA),
            entry("RUH", "SA", "사우디아라비아", "리야드", 24.9576, 46.6988, Continent.ASIA),
            entry("AMM", "JO", "요르단", "암만", 31.7226, 35.9932, Continent.ASIA),
            entry("TLV", "IL", "이스라엘", "텔아비브", 32.0114, 34.8867, Continent.ASIA),
            // ── 터키 ──────────────────────────────────────────────────────────
            entry("IST", "TR", "터키", "이스탄불", 41.2753, 28.7519, Continent.EUROPE),
            entry("SAW", "TR", "터키", "이스탄불", 40.8985, 29.3092, Continent.EUROPE),
            // ── 유럽 ──────────────────────────────────────────────────────────
            entry("LHR", "GB", "영국", "런던", 51.4775, -0.4614, Continent.EUROPE),
            entry("LGW", "GB", "영국", "런던", 51.1537, -0.1821, Continent.EUROPE),
            entry("CDG", "FR", "프랑스", "파리", 49.0097, 2.5478, Continent.EUROPE),
            entry("ORY", "FR", "프랑스", "파리", 48.7262, 2.3652, Continent.EUROPE),
            entry("FRA", "DE", "독일", "프랑크푸르트", 50.0379, 8.5622, Continent.EUROPE),
            entry("MUC", "DE", "독일", "뮌헨", 48.3537, 11.7750, Continent.EUROPE),
            entry("AMS", "NL", "네덜란드", "암스테르담", 52.3105, 4.7683, Continent.EUROPE),
            entry("MAD", "ES", "스페인", "마드리드", 40.4936, -3.5673, Continent.EUROPE),
            entry("BCN", "ES", "스페인", "바르셀로나", 41.2974, 2.0833, Continent.EUROPE),
            entry("FCO", "IT", "이탈리아", "로마", 41.7999, 12.2462, Continent.EUROPE),
            entry("MXP", "IT", "이탈리아", "밀라노", 45.6306, 8.7281, Continent.EUROPE),
            entry("VCE", "IT", "이탈리아", "베네치아", 45.5053, 12.3519, Continent.EUROPE),
            entry("VIE", "AT", "오스트리아", "빈", 48.1102, 16.5697, Continent.EUROPE),
            entry("ZRH", "CH", "스위스", "취리히", 47.4582, 8.5555, Continent.EUROPE),
            entry("GVA", "CH", "스위스", "제네바", 46.2381, 6.1089, Continent.EUROPE),
            entry("BRU", "BE", "벨기에", "브뤼셀", 50.9010, 4.4844, Continent.EUROPE),
            entry("CPH", "DK", "덴마크", "코펜하겐", 55.6180, 12.6561, Continent.EUROPE),
            entry("ARN", "SE", "스웨덴", "스톡홀름", 59.6519, 17.9186, Continent.EUROPE),
            entry("OSL", "NO", "노르웨이", "오슬로", 60.1939, 11.0998, Continent.EUROPE),
            entry("HEL", "FI", "핀란드", "헬싱키", 60.3172, 24.9633, Continent.EUROPE),
            entry("DUB", "IE", "아일랜드", "더블린", 53.4213, -6.2701, Continent.EUROPE),
            entry("LIS", "PT", "포르투갈", "리스본", 38.7813, -9.1359, Continent.EUROPE),
            entry("ATH", "GR", "그리스", "아테네", 37.9364, 23.9445, Continent.EUROPE),
            entry("PRG", "CZ", "체코", "프라하", 50.1008, 14.2600, Continent.EUROPE),
            entry("WAW", "PL", "폴란드", "바르샤바", 52.1657, 20.9671, Continent.EUROPE),
            entry("BUD", "HU", "헝가리", "부다페스트", 47.4298, 19.2611, Continent.EUROPE),
            entry("SVO", "RU", "러시아", "모스크바", 55.9736, 37.4125, Continent.EUROPE),
            entry("LED", "RU", "러시아", "상트페테르부르크", 59.8003, 30.2625, Continent.EUROPE),
            // ── 북아메리카 ────────────────────────────────────────────────────
            entry("JFK", "US", "미국", "뉴욕", 40.6413, -73.7781, Continent.NORTH_AMERICA),
            entry("EWR", "US", "미국", "뉴욕", 40.6895, -74.1745, Continent.NORTH_AMERICA),
            entry("LAX", "US", "미국", "로스앤젤레스", 33.9425, -118.4081, Continent.NORTH_AMERICA),
            entry("SFO", "US", "미국", "샌프란시스코", 37.6213, -122.3790, Continent.NORTH_AMERICA),
            entry("ORD", "US", "미국", "시카고", 41.9742, -87.9073, Continent.NORTH_AMERICA),
            entry("ATL", "US", "미국", "애틀랜타", 33.6407, -84.4277, Continent.NORTH_AMERICA),
            entry("DFW", "US", "미국", "댈러스", 32.8998, -97.0403, Continent.NORTH_AMERICA),
            entry("MIA", "US", "미국", "마이애미", 25.7959, -80.2870, Continent.NORTH_AMERICA),
            entry("SEA", "US", "미국", "시애틀", 47.4502, -122.3088, Continent.NORTH_AMERICA),
            entry("LAS", "US", "미국", "라스베이거스", 36.0840, -115.1537, Continent.NORTH_AMERICA),
            entry("HNL", "US", "미국", "호놀룰루", 21.3187, -157.9224, Continent.NORTH_AMERICA),
            entry("YYZ", "CA", "캐나다", "토론토", 43.6777, -79.6248, Continent.NORTH_AMERICA),
            entry("YVR", "CA", "캐나다", "밴쿠버", 49.1947, -123.1792, Continent.NORTH_AMERICA),
            entry("YUL", "CA", "캐나다", "몬트리올", 45.4706, -73.7408, Continent.NORTH_AMERICA),
            entry("MEX", "MX", "멕시코", "멕시코시티", 19.4363, -99.0721, Continent.NORTH_AMERICA),
            entry("CUN", "MX", "멕시코", "칸쿤", 21.0365, -86.8771, Continent.NORTH_AMERICA),
            entry("GUM", "GU", "괌", "괌", 13.4834, 144.7958, Continent.OCEANIA),
            // ── 남아메리카 ────────────────────────────────────────────────────
            entry("GRU", "BR", "브라질", "상파울루", -23.4356, -46.4731, Continent.SOUTH_AMERICA),
            entry("GIG", "BR", "브라질", "리우데자네이루", -22.8100, -43.2506, Continent.SOUTH_AMERICA),
            entry("BOG", "CO", "콜롬비아", "보고타", 4.7016, -74.1469, Continent.SOUTH_AMERICA),
            entry("LIM", "PE", "페루", "리마", -12.0219, -77.1143, Continent.SOUTH_AMERICA),
            entry("SCL", "CL", "칠레", "산티아고", -33.3930, -70.7858, Continent.SOUTH_AMERICA),
            entry("EZE", "AR", "아르헨티나", "부에노스아이레스", -34.8222, -58.5358, Continent.SOUTH_AMERICA),
            // ── 아프리카 ──────────────────────────────────────────────────────
            entry("CAI", "EG", "이집트", "카이로", 30.1219, 31.4056, Continent.AFRICA),
            entry("NBO", "KE", "케냐", "나이로비", -1.3192, 36.9275, Continent.AFRICA),
            entry("LOS", "NG", "나이지리아", "라고스", 6.5774, 3.3214, Continent.AFRICA),
            entry("JNB", "ZA", "남아프리카공화국", "요하네스버그", -26.1392, 28.2460, Continent.AFRICA),
            entry("CPT", "ZA", "남아프리카공화국", "케이프타운", -33.9648, 18.6017, Continent.AFRICA),
            entry("CMN", "MA", "모로코", "카사블랑카", 33.3675, -7.5898, Continent.AFRICA),
            entry("ADD", "ET", "에티오피아", "아디스아바바", 8.9779, 38.7993, Continent.AFRICA),
            // ── 오세아니아 ────────────────────────────────────────────────────
            entry("SYD", "AU", "호주", "시드니", -33.9461, 151.1772, Continent.OCEANIA),
            entry("MEL", "AU", "호주", "멜버른", -37.6690, 144.8410, Continent.OCEANIA),
            entry("BNE", "AU", "호주", "브리즈번", -27.3842, 153.1175, Continent.OCEANIA),
            entry("PER", "AU", "호주", "퍼스", -31.9403, 115.9669, Continent.OCEANIA),
            entry("AKL", "NZ", "뉴질랜드", "오클랜드", -37.0082, 174.7850, Continent.OCEANIA),
            entry("CHC", "NZ", "뉴질랜드", "크라이스트처치", -43.4894, 172.5320, Continent.OCEANIA)
    );

    private static Map.Entry<String, AirportInfo> entry(
            String iata, String countryCode, String countryNameKo,
            String cityName, double lat, double lng, Continent continent) {
        return Map.entry(iata, new AirportInfo(iata, countryCode, countryNameKo, cityName, lat, lng, continent));
    }

    public Optional<AirportInfo> findByIata(String iataCode) {
        if (iataCode == null) return Optional.empty();
        return Optional.ofNullable(AIRPORT_MAP.get(iataCode.toUpperCase()));
    }

    // ── 국가코드 → 대륙 매핑 (수동 등록 시 사용) ──────────────────────────
    private static final Map<String, Continent> COUNTRY_CONTINENT_MAP = Map.ofEntries(
            Map.entry("KR", Continent.ASIA), Map.entry("JP", Continent.ASIA),
            Map.entry("CN", Continent.ASIA), Map.entry("TW", Continent.ASIA),
            Map.entry("HK", Continent.ASIA), Map.entry("MO", Continent.ASIA),
            Map.entry("TH", Continent.ASIA), Map.entry("VN", Continent.ASIA),
            Map.entry("SG", Continent.ASIA), Map.entry("MY", Continent.ASIA),
            Map.entry("PH", Continent.ASIA), Map.entry("ID", Continent.ASIA),
            Map.entry("MM", Continent.ASIA), Map.entry("KH", Continent.ASIA),
            Map.entry("LA", Continent.ASIA), Map.entry("IN", Continent.ASIA),
            Map.entry("LK", Continent.ASIA), Map.entry("NP", Continent.ASIA),
            Map.entry("BD", Continent.ASIA), Map.entry("PK", Continent.ASIA),
            Map.entry("AE", Continent.ASIA), Map.entry("QA", Continent.ASIA),
            Map.entry("SA", Continent.ASIA), Map.entry("JO", Continent.ASIA),
            Map.entry("IL", Continent.ASIA), Map.entry("MN", Continent.ASIA),
            Map.entry("KZ", Continent.ASIA), Map.entry("UZ", Continent.ASIA),
            Map.entry("TR", Continent.EUROPE), Map.entry("GB", Continent.EUROPE),
            Map.entry("FR", Continent.EUROPE), Map.entry("DE", Continent.EUROPE),
            Map.entry("NL", Continent.EUROPE), Map.entry("ES", Continent.EUROPE),
            Map.entry("IT", Continent.EUROPE), Map.entry("AT", Continent.EUROPE),
            Map.entry("CH", Continent.EUROPE), Map.entry("BE", Continent.EUROPE),
            Map.entry("DK", Continent.EUROPE), Map.entry("SE", Continent.EUROPE),
            Map.entry("NO", Continent.EUROPE), Map.entry("FI", Continent.EUROPE),
            Map.entry("IE", Continent.EUROPE), Map.entry("PT", Continent.EUROPE),
            Map.entry("GR", Continent.EUROPE), Map.entry("CZ", Continent.EUROPE),
            Map.entry("PL", Continent.EUROPE), Map.entry("HU", Continent.EUROPE),
            Map.entry("RU", Continent.EUROPE), Map.entry("RO", Continent.EUROPE),
            Map.entry("HR", Continent.EUROPE), Map.entry("RS", Continent.EUROPE),
            Map.entry("SK", Continent.EUROPE), Map.entry("SI", Continent.EUROPE),
            Map.entry("UA", Continent.EUROPE), Map.entry("BG", Continent.EUROPE),
            Map.entry("US", Continent.NORTH_AMERICA), Map.entry("CA", Continent.NORTH_AMERICA),
            Map.entry("MX", Continent.NORTH_AMERICA), Map.entry("GU", Continent.OCEANIA),
            Map.entry("BR", Continent.SOUTH_AMERICA), Map.entry("CO", Continent.SOUTH_AMERICA),
            Map.entry("PE", Continent.SOUTH_AMERICA), Map.entry("CL", Continent.SOUTH_AMERICA),
            Map.entry("AR", Continent.SOUTH_AMERICA), Map.entry("EC", Continent.SOUTH_AMERICA),
            Map.entry("VE", Continent.SOUTH_AMERICA), Map.entry("BO", Continent.SOUTH_AMERICA),
            Map.entry("PY", Continent.SOUTH_AMERICA), Map.entry("UY", Continent.SOUTH_AMERICA),
            Map.entry("EG", Continent.AFRICA), Map.entry("KE", Continent.AFRICA),
            Map.entry("NG", Continent.AFRICA), Map.entry("ZA", Continent.AFRICA),
            Map.entry("MA", Continent.AFRICA), Map.entry("ET", Continent.AFRICA),
            Map.entry("TZ", Continent.AFRICA), Map.entry("GH", Continent.AFRICA),
            Map.entry("AU", Continent.OCEANIA), Map.entry("NZ", Continent.OCEANIA)
    );

    public Continent getContinentByCountryCode(String countryCode) {
        return COUNTRY_CONTINENT_MAP.getOrDefault(countryCode, Continent.ASIA);
    }

    // 두 좌표 간 거리 계산 (Haversine formula, km)
    public double calculateDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
