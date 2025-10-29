package com.example.culture_archive.web.controller;

import com.example.culture_archive.external.llm.OllamaService;
import com.example.culture_archive.external.ocr.OcrService;
import com.example.culture_archive.util.text.AllowedKeywords;
import com.example.culture_archive.util.text.KeywordExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OllamaTestController {

    private final OcrService ocrService;
    private final OllamaService ollamaService;
    private final KeywordExtractor keywordExtractor;

    @GetMapping("/dev/ollama")
    public String form() {
        return "dev/ollama-test"; // templates/dev/ollama-test.html
    }

    @PostMapping("/dev/ollama/analyze")
    public String analyze(@RequestParam("imageFile") MultipartFile imageFile,
                          @RequestParam(value = "prompt", required = false) String prompt,
                          Model model) throws IOException {
        if (imageFile == null || imageFile.isEmpty()) {
            model.addAttribute("error", "이미지를 선택하세요.");
            return "dev/ollama-test";
        }

        // 1) OCR
        String ocrText = ocrService.getTextFromImage(imageFile);

        // 2) 허용 키워드 준비
        List<String> allow = AllowedKeywords.all();

        // 3) LLaVA 프롬프트(영문, 한국어 응답 강제)
        // 3) LLaVA 프롬프트(영문 지시, 한국어 섹션 + 마지막 JSON 배열 필수)
        String allowedJoined = String.join(", ", allow);
        String defaultPrompt = """
                You are a keyword selector for Korean culture events.
                Use ONLY the exact tokens from the allowed list. If you infer variants or synonyms
                (e.g., animation/Anime/애니메), map them to the canonical token in the list.
                
                Task:
                1) Read the OCR text and the image.
                2) Infer title/venue/date only if they are visibly present on the poster/ticket.
                3) Choose 5–10 keywords USING ONLY the tokens in [Allowed].
                4) At the end, output a SINGLE JSON array of chosen tokens. No extra text after the array.
                
                [Allowed]
                %s
                
                [OCR]
                %s
                
                [Output in Korean]
                - 제목 후보(보이는 텍스트에서만, 1–3개):
                - 장소/지역(보이는 정보에서만):
                - 기간(가능하면 yyyy.MM.dd, 보이는 정보에서만):
                - 키워드(허용 리스트에서만, 5–10개):
                
                [KEYWORDS_JSON]
                (여기에만 JSON 배열. 예: ["애니메이션","전시","서울"])
                """.formatted(allowedJoined, ocrText);


        // 4) LLaVA 호출
        String llavaRaw = ollamaService.analyzeImage(
                imageFile,
                (prompt == null || prompt.isBlank()) ? defaultPrompt : prompt
        );

        // 5) 화이트리스트 기반 키워드 선택(LLM 결과 우선, 실패시 OCR+LLM, 최후엔 정규식 매칭)
        List<String> llavaChosen = ollamaService.suggestKeywordsFromText(llavaRaw, allow);
        if (llavaChosen.isEmpty()) {
            llavaChosen = ollamaService.suggestKeywordsFromText(ocrText + " " + llavaRaw, allow);
        }
        List<String> fallbackChosen = keywordExtractor.extract(allow, ocrText, llavaRaw);

        // 6) 뷰 모델
        model.addAttribute("ocrText", ocrText);
        model.addAttribute("llavaRaw", llavaRaw);
        model.addAttribute("llavaChosen", llavaChosen);
        model.addAttribute("fallbackChosen", fallbackChosen);
        model.addAttribute("prompt", (prompt == null || prompt.isBlank()) ? defaultPrompt : prompt);

        return "dev/ollama-test";
    }

}
