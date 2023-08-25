package com.foodspring.foodspringmailer.service;
/*
IntelliJ IDEA 2022.2.2 (Community Edition)
Build #IC-222.4167.29, built on September 13, 2022
Runtime version: 17.0.4+7-b469.53 amd64
@Author hakim a.k.a. Hakim Amarullah
Java Developer
Created on 8/25/2023 6:16 AM
@Last Modified 8/25/2023 6:16 AM
Version 1.0
*/

import com.foodspring.constant.EmailType;
import com.foodspring.foodspringmailer.core.SMTPCore;
import com.foodspring.model.EmailVerification;
import com.foodspring.utils.LoggingFile;
import com.foodspring.utils.ReadTextFileSB;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.foodspring.constant.EmailType.*;

@Service
public class MailerService {
    private final Logger logger = LoggerFactory.getLogger(MailerService.class);

    private String[] strException = new String[2];
    private String senderEmail;
    private Map<Enum<EmailType>, String> mailTemplate = new HashMap<>();

    public MailerService() {
        mailTemplate.put(VER_NEW_TOKEN_EMAIL, "\\data\\ver_token_baru.html");
        mailTemplate.put(VER_REGIS, "\\data\\ver_regis.html");
        mailTemplate.put(FORGOT_PASSWORD, "\\data\\ver_lupa_pwd.html");
        strException[0] = "MailerService";

    }


    @Retryable(retryFor = {Exception.class}, maxAttempts = 2)
    @RabbitListener(queues = "email-verification-queue")
    public void sendSMTPToken(@Payload EmailVerification emailVerification, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        try {
            String strContent = new ReadTextFileSB(mailTemplate.get(emailVerification.getEmailType())).getContentFile();
            strContent = strContent.replace("#JKVM3NH", emailVerification.getSubject());//Kepentingan
            strContent = strContent.replace("XF#31NN", emailVerification.getFullName());//Nama Lengkap
            strContent = strContent.replace("8U0_1GH$", emailVerification.getVerification());//TOKEN

            String[] strEmail = {emailVerification.getEmail()};
            SMTPCore sc = new SMTPCore();
            boolean success = sc.sendMailWithAttachment(strEmail,
                    emailVerification.getSubject(),
                    strContent,
                    "SSL", null);
            if (!success) {
                throw new Exception("Can't send email");
            }
            channel.basicAck(tag, false);
            logger.info("Email sent!");

        } catch (Exception e) {
            strException[1] = "sendToken(String mailAddress, String subject, String purpose, String token) -- LINE 38";
            LoggingFile.exceptionString(strException, e, "y");
            channel.basicReject(tag, true);
            throw new Exception(e.getMessage());
        }
    }

    @Recover
    public void recover(Exception e, EmailVerification emailMessage) {
        logger.warn("RETRYING");
        logger.error(e.getMessage());
    }
}



