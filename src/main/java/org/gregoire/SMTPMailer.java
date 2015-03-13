package org.gregoire;

import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SMTPMailer {

    private static final String SMTP_HOST_NAME = "smtp.gmail.com";
    private static final int SMTP_HOST_PORT = 465;
    private static final String SMTP_AUTH_USER = "mysmtpdev@gmail.com";
    private static final String SMTP_AUTH_PWD = "aZ8eW0zihIHQaKUwdCZ6";
    
    public static void send(String recipient, String sequence) throws Exception {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtps");
        props.put("mail.smtps.host", SMTP_HOST_NAME);
        props.put("mail.smtps.auth", "true");

        Session mailSession = Session.getDefaultInstance(props);
        mailSession.setDebug(true);
        Transport transport = mailSession.getTransport();

        MimeMessage message = new MimeMessage(mailSession);
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        message.setReplyTo(new Address[] { new InternetAddress(recipient) });
        message.setHeader("X-Mailer", "Privacy Lock");
        message.setSentDate(new Date());
        message.setSubject("Privacy Lock - New unlock code");
        message.setContent(String.format("Your requested unlock sequence: %s", sequence), "text/plain");        
        transport.connect(SMTP_HOST_NAME, SMTP_HOST_PORT, SMTP_AUTH_USER, SMTP_AUTH_PWD);
        transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
        transport.close();
    }
    
}