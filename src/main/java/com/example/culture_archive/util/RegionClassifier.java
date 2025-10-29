package com.example.culture_archive.util;

import org.springframework.stereotype.Component;

@Component
public class RegionClassifier {

    public String classify(String eventSite) {
        if (eventSite == null || eventSite.isBlank()) {
            return "기타";
        }

        // 1. 우선순위 조정: 가장 구체적인 '서울'부터 확인하여 다른 지역으로 잘못 분류되는 것을 방지합니다.
        if (containsAny(eventSite,
                "서울", "종로", "중구", "용산", "성동", "광진", "동대문", "중랑", "성북", "강북",
                "도봉", "노원", "은평", "서대문", "마포", "양천", "강서", "구로", "금천", "영등포",
                "동작", "관악", "서초", "강남", "송파", "강동",
                "대학로", "홍대", "코엑스", "예술의전전당", "세종문화회관", "DDP"
        )) return "서울";

        // 2. 경기도 광주 예외 처리: '광주'를 포함하지만 '경기도'가 아닐 때만 광주광역시로 분류합니다.
        if (containsAny(eventSite, "광주") && !eventSite.contains("경기도")) return "광주";

        // 3. 나머지 지역들을 확인합니다.
        if (containsAny(eventSite, "부산", "해운대", "센텀")) return "부산";
        if (containsAny(eventSite, "대구")) return "대구";
        if (containsAny(eventSite, "인천")) return "인천";
        if (containsAny(eventSite, "대전")) return "대전";
        if (containsAny(eventSite, "울산")) return "울산";
        if (containsAny(eventSite, "세종")) return "세종";
        if (containsAny(eventSite, "경기", "수원", "고양", "성남", "용인", "안산", "부천", "안양", "화성", "남양주", "의정부", "평택", "파주")) return "경기";
        if (containsAny(eventSite, "강원", "춘천", "강릉", "원주", "속초")) return "강원";
        if (containsAny(eventSite, "충북", "충청북도", "청주")) return "충북";
        if (containsAny(eventSite, "충남", "충청남도", "천안", "아산")) return "충남";
        if (containsAny(eventSite, "전북", "전라북도", "전주")) return "전북";
        if (containsAny(eventSite, "전남", "전라남도", "여수", "순천")) return "전남";
        if (containsAny(eventSite, "경북", "경상북도", "포항", "경주")) return "경북";
        if (containsAny(eventSite, "경남", "경상남도", "창원", "김해", "진주")) return "경남";
        if (containsAny(eventSite, "제주")) return "제주";

        return "기타";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}