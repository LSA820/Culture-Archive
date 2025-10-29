package com.example.culture_archive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;

    public void sendPasswordResetMail(String to, String token) {
        String subject = "Culture Archive 비밀번호 재설정";
        String resetLink = "https://your-domain.com/member/reset?token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText("아래 링크를 눌러 비밀번호를 재설정하세요:\n\n" + resetLink);
        mailSender.send(msg);
    }

    /** 제목/본문 그대로 보내는 단순 메일 */
    public void sendSimple(String to, String subject, String body){
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }
}
