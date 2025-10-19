package com.fptuni.vms.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for template operations
 */
@Component("templateUtils")
public class TemplateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Format LocalDateTime to date string
     */
    public String formatDate(LocalDateTime dateTime) {
        if (dateTime == null)
            return "";
        return dateTime.format(DATE_FORMATTER);
    }

    /**
     * Format LocalDateTime to datetime string
     */
    public String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null)
            return "";
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * Truncate text to specified length
     */
    public String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        if (text.length() <= maxLength)
            return text;
        return text.substring(0, maxLength) + "...";
    }

    /**
     * Get default thumbnail if null
     */
    public String getDefaultThumbnail(String thumbnailUrl) {
        if (thumbnailUrl != null && !thumbnailUrl.trim().isEmpty()) {
            return thumbnailUrl;
        }
        return "https://images.pexels.com/photos/8613089/pexels-photo-8613089.jpeg?auto=compress&cs=tinysrgb&w=800";
    }
}