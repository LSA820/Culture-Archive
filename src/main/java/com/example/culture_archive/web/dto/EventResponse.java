package com.example.culture_archive.web.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class EventResponse {
    private Header header;
    private Body body;

    @Getter @Setter public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter @Setter public static class Body {
        private Items items;
    }

    @Getter @Setter public static class Items {
        private List<Event> item;   // 목록
    }

    @Getter @Setter public static class Event {
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
