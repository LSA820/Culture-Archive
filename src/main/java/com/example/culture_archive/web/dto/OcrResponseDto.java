package com.example.culture_archive.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // JSON 응답에 모르는 필드가 있어도 무시합니다.
public class OcrResponseDto {
    private List<Image> images;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {
        private String inferResult;
        private List<Field> fields;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Field {
        private String inferText; // 실제 텍스트 조각
        private boolean lineBreak; // 줄바꿈 여부
    }
}