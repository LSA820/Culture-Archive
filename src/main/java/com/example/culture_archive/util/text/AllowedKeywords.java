// src/main/java/com/example/culture_archive/service/AllowedKeywords.java
package com.example.culture_archive.util.text;

import java.util.List;

public final class AllowedKeywords {
    private AllowedKeywords() {}

    // 장르
    public static final List<String> GENRE = List.of(
            "전시","공연","뮤지컬","콘서트","연극"
    );

    // 지역(광역)
    public static final List<String> REGION = List.of(
            "서울","경기","인천","부산","대구","광주","대전","울산","세종",
            "강원","충북","충남","전북","전남","경북","경남","제주"
    );

    // 대상/연령
    public static final List<String> AUDIENCE = List.of(
            "어린이","가족","청소년","성인","시니어"
    );

    // 무드/스타일
    public static final List<String> STYLE = List.of(
            "감성적","화려한","클래식","현대","애니메이션","사진",
            "미디어아트","설치","체험형"
    );

    // 주제
    public static final List<String> THEME = List.of(
            "역사","과학","자연","캐릭터","K-팝","게임","영화"
    );

    // 장소 유형
    public static final List<String> VENUE = List.of(
            "미술관","갤러리","박물관","공연장","야외"
    );

    // 한 벌로 전달할 전체 리스트
    public static List<String> all() {
        return List.of(
                String.join(",", GENRE),
                String.join(",", REGION),
                String.join(",", AUDIENCE),
                String.join(",", STYLE),
                String.join(",", THEME),
                String.join(",", VENUE)
        ).stream().flatMap(s -> List.of(s.split(",")).stream()).toList();
    }
}
