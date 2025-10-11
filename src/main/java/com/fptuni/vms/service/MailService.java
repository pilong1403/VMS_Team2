package com.fptuni.vms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Gửi email khi hồ sơ bị từ chối
     */
    public void sendRejectEmail(String to, String orgName, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Kết quả xét duyệt hồ sơ tổ chức: " + orgName);
        message.setText("Xin chào,\n\nHồ sơ '" + orgName + "' đã bị từ chối."
                + (reason != null && !reason.isBlank() ? "\n\nLý do: " + reason : "")
                + "\n\nTrân trọng,\nBan quản trị hệ thống.");
        mailSender.send(message);
    }

    /**
     * Gửi email khi hồ sơ được duyệt
     */
    public void sendApproveEmail(String to, String orgName, String note) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Kết quả xét duyệt hồ sơ tổ chức: " + orgName);
        message.setText("Xin chào,\n\nHồ sơ '" + orgName + "' đã được DUYỆT."
                + (note != null && !note.isBlank() ? "\n\nGhi chú: " + note : "")
                + "\n\nTrân trọng,\nBan quản trị hệ thống.");
        mailSender.send(message);
    }
}
