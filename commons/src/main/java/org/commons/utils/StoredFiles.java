package org.commons.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Checks for files people upload (payslips, logos, photos) before they are stored.
 * The type is decided from the file's own bytes, never from the name or the
 * browser-supplied content type, so a renamed script or HTML page is rejected.
 */
public final class StoredFiles {

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    public static final String PDF = "application/pdf";
    public static final String PNG = "image/png";
    public static final String JPEG = "image/jpeg";

    private StoredFiles() {
    }

    /** The content type the bytes really are (PDF, PNG or JPEG), or null for anything else. */
    public static String detectContentType(byte[] data) {
        if (data == null || data.length < 4) return null;
        if (startsWith(data, "%PDF-".getBytes(StandardCharsets.US_ASCII))) return PDF;
        if (startsWith(data, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})) return PNG;
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) return JPEG;
        return null;
    }

    /**
     * Why the upload cannot be accepted, in words a user can act on, or null if it is fine.
     */
    public static String rejectionReason(byte[] data, boolean imagesOnly) {
        if (data == null || data.length == 0) return "The file is empty.";
        if (data.length > MAX_BYTES) return "The file is larger than 5 MB.";
        String type = detectContentType(data);
        if (type == null || (imagesOnly && PDF.equals(type))) {
            return imagesOnly ? "Only PNG or JPEG images can be uploaded." : "Only PDF, PNG or JPEG files can be uploaded.";
        }
        return null;
    }

    /** A file name safe to show and to put in a Content-Disposition header. */
    public static String safeFileName(String original, String contentType) {
        String base = original == null ? "" : original.replaceAll("^.*[\\\\/]", "");
        base = base.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (base.isEmpty() || base.startsWith(".")) base = "document";
        if (base.length() > 120) base = base.substring(base.length() - 120);
        String ext = PDF.equals(contentType) ? ".pdf" : PNG.equals(contentType) ? ".png" : JPEG.equals(contentType) ? ".jpg" : "";
        if (!ext.isEmpty() && !base.toLowerCase().endsWith(ext) && !(ext.equals(".jpg") && base.toLowerCase().endsWith(".jpeg"))) {
            base = base + ext;
        }
        return base;
    }

    public static String sha256Hex(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
