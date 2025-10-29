package com.example.culture_archive.external.ocr;

import com.example.culture_archive.web.dto.OcrResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder; // MultipartBodyBuilder import
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException; // IOException import
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

    private final WebClient webClient = WebClient.create();

    @Value("${naver.ocr.api.url}")
    private String apiUrl;

    @Value("${naver.ocr.api.secret-key}")
    private String secretKey;

    public String getTextFromImage(MultipartFile imageFile) {
        // Multipart 요청 본문을 만들기 위한 Builder 생성
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        // 1. JSON 메시지 파트 추가
        String requestBody = String.format("""
            {
              "images": [{"format": "%s", "name": "image"}],
              "requestId": "%s", "version": "V2", "timestamp": %d
            }""",
                imageFile.getContentType().split("/")[1],
                UUID.randomUUID().toString(),
                System.currentTimeMillis()
        );
        builder.part("message", requestBody, MediaType.APPLICATION_JSON);

        // 👇 [핵심 수정] 2. 파일 파트를 Resource 대신 byte[] 와 filename으로 직접 추가합니다.
        try {
            builder.part("file", imageFile.getBytes())
                    .filename(imageFile.getOriginalFilename());
        } catch (IOException e) {
            log.error("Failed to read image file", e);
            throw new RuntimeException("이미지 파일을 읽는 데 실패했습니다.", e);
        }

        // 3. WebClient로 요청 전송
        OcrResponseDto response = webClient.post()
                .uri(apiUrl)
                .header("X-OCR-SECRET", secretKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build()) // 완성된 본문을 전달
                .retrieve()
                .bodyToMono(OcrResponseDto.class)
                .block();

        // 4. 결과에서 텍스트 추출 (기존과 동일)
        StringBuilder extractedText = new StringBuilder();
        if (response != null && response.getImages() != null) {
            for (OcrResponseDto.Image image : response.getImages()) {
                for (OcrResponseDto.Field field : image.getFields()) {
                    extractedText.append(field.getInferText()).append(" ");
                }
            }
        }

        log.info(">>>> OCR Extracted Text: {}", extractedText.toString());
        return extractedText.toString();
    }
}