package com.example.culture_archive.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class OllamaApiDto {

    // Ollama API '/api/generate' 요청 본문 DTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationRequest {
        private String model;          // 사용할 모델 이름 (예: "llava")
        private String prompt;         // 이미지와 함께 보낼 질문
        private List<String> images;   // Base64 인코딩된 이미지 데이터 리스트
        private boolean stream = false; // 응답을 스트리밍하지 않음 (한 번에 받음)
    }

    // Ollama API '/api/generate' 응답 본문 DTO
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true) // 모르는 필드는 무시
    public static class GenerationResponse {
        private String model;
        private String created_at;
        private String response; // AI 모델이 생성한 실제 텍스트 답변
        private boolean done;
        // (그 외 응답 필드들은 무시됨)
    }
}