package com.example.culture_archive.web.controller;

import com.example.culture_archive.external.llm.OllamaService;
import com.example.culture_archive.service.event.EventService;
import com.example.culture_archive.external.ocr.OcrService;
import com.example.culture_archive.util.text.AllowedKeywords;
import com.example.culture_archive.web.dto.EventCardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;
    private final OllamaService ollamaService;
    private final EventService eventService;

    // 로그인 선확인. 미인증이면 로그인 페이지로 리다이렉트
    @GetMapping("/ocr/upload")
    public String uploadForm(Principal principal) {
        if (principal == null) return "redirect:/member/login?redirect=/ocr/upload";
        return "ocr/ocr-upload";
    }

    // 업로드 처리 → OCR → 후보 매칭 → 결과
    @PostMapping("/ocr/process")
    public String process(@RequestParam("imageFile") MultipartFile imageFile,
                          Principal principal, Model model) throws IOException {
        if (principal == null) return "redirect:/member/login?redirect=/ocr/upload";
        if (imageFile == null || imageFile.isEmpty()) {
            model.addAttribute("error","이미지 파일을 선택하세요.");
            return "ocr/ocr-upload";
        }

        // OCR 전체 텍스트
        String ocrText = ocrService.getTextFromImage(imageFile);

        // LLaVA: 제목 후보 + 키워드(허용 목록 유지)
        var allowed = AllowedKeywords.all(); // 기존 목록
        var nlp = ollamaService.analyzeImageForTermsAndKeywords(imageFile, ocrText, allowed);

        // 검색: OCR 전체문 + 제목후보(OR) → 병합
        var fromOcr   = eventService.findEventsByOcrText(ocrText);
        var fromTerms = eventService.findEventsByAnyTerms(nlp.titleTerms());
        var candidates = java.util.stream.Stream.concat(fromOcr.stream(), fromTerms.stream())
                .collect(java.util.stream.Collectors.toMap(
                        EventCardDto::id, e->e, (a,b)->a, java.util.LinkedHashMap::new))
                .values().stream().toList();

        model.addAttribute("extractedText", ocrText);
        model.addAttribute("llavaTerms", nlp.titleTerms());
        model.addAttribute("keywords", nlp.keywords());   // 태깅·추천에 사용
        model.addAttribute("candidates", candidates);

        return "ocr/ocr-result";
    }


    // 유지: 기존 테스트 엔드포인트 필요 시
    @GetMapping("/ocr-test")
    public String ocrTestForm() { return "ocr-test"; }
}
