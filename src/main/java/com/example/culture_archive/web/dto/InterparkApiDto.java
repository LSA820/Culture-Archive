package com.example.culture_archive.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterparkApiDto {


    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoodsItem {
        // 공연/전시 이름
        private String goodsName;
        // 장소
        private String placeName;

        @JsonAlias({"playStartDate","startDate","sDate"})
        private String playStartDate;

        @JsonAlias({"playEndDate","endDate","eDate"})
        private String playEndDate;

        @JsonAlias({"imageUrl","posterLarge","poster","posterUrl"})
        private String imageUrl;

        private String goodsCode;

        // 장르
        private String kindOfGoodsName;
    }
}