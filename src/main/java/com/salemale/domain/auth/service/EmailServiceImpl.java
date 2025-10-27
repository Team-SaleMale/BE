package com.salemale.domain.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * EmailServiceImpl: 이메일 전송 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendPasswordResetCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("[SaleMale] 비밀번호 재설정 인증번호");

            String htmlContent = buildPasswordResetCodeEmailHtml(code);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("비밀번호 재설정 인증번호 이메일 전송 완료: {}", maskEmail(to));
        } catch (MessagingException | MailException e) {
            log.error("비밀번호 재설정 인증번호 이메일 전송 실패: {}", maskEmail(to), e);
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }
    
    /**
     * 이메일을 마스킹하여 로그에 기록합니다.
     * 
     * @param email 원본 이메일 주소
     * @return 마스킹된 이메일 (예: g***@naver.com)
     */
    private String maskEmail(String email) {
        if (email == null) return "null";
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }

    private String buildPasswordResetCodeEmailHtml(String code) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { padding: 40px; }
                    .code { font-size: 48px; font-weight: bold; color: #667eea; text-align: center; letter-spacing: 12px; margin: 30px 0; background-color: #f8f9fa; padding: 30px; border-radius: 10px; }
                    .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                    .footer { padding: 20px; text-align: center; color: #999; font-size: 12px; border-top: 1px solid #eee; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>SaleMale 비밀번호 재설정</h1>
                    </div>
                    <div class="content">
                        <p>안녕하세요!</p>
                        <p>비밀번호 재설정을 위한 인증번호입니다.</p>
                        <div class="code">%s</div>
                        <div class="warning">
                            <strong>⚠️ 보안 안내</strong>
                            <p>• 이 인증번호는 10분간만 유효합니다.</p>
                            <p>• 본인이 요청하지 않았다면 이 이메일을 무시하세요.</p>
                            <p>• 인증번호는 타인에게 공유하지 마세요.</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p>© 2025 valuebid. All rights reserved.</p>
                        <p>이 메일은 발신 전용입니다.</p>
                    </div>
                </div>
            </body>
            </html>
            """, code);
    }
}

