package com.example.culture_diary.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.*;
import lombok.Data;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "response")
@JsonIgnoreProperties(ignoreUnknown = true) // 모르는 태그는 무시
public class KcisaXml {
    private Header header;  // ← 추가
    private Body body;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        @JacksonXmlProperty(localName = "items")
        private Items items;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        private List<Event> item;

        // 필요하면 totalCount, pageNo, numOfRows 도 여기에 선언 가능
        private String totalCount;
        private String pageNo;
        private String numOfRows;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {
        private String title;
        private String type;
        private String period;
        private String eventPeriod;
        private String eventSite;
        private String charge;
        private String contactPoint;
        private String url;
        private String imageObject;
        private String description;
        private String viewCount;
    }
}
