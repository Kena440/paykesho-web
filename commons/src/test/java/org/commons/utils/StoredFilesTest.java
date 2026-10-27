package org.commons.utils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StoredFilesTest {

    private static final byte[] PDF = "%PDF-1.4\n%%EOF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};

    @Test
    void typeComesFromTheBytes() {
        assertEquals(StoredFiles.PDF, StoredFiles.detectContentType(PDF));
        assertEquals(StoredFiles.PNG, StoredFiles.detectContentType(PNG));
        assertEquals(StoredFiles.JPEG, StoredFiles.detectContentType(JPEG));
        assertNull(StoredFiles.detectContentType("<html><script>".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void payslipsMayBePdfOrImages() {
        assertNull(StoredFiles.rejectionReason(PDF, false));
        assertNull(StoredFiles.rejectionReason(JPEG, false));
        assertNotNull(StoredFiles.rejectionReason("MZ\u0090\u0000 executable".getBytes(StandardCharsets.ISO_8859_1), false));
        assertNotNull(StoredFiles.rejectionReason(new byte[0], false));
    }

    @Test
    void logosMustBeImages() {
        assertNotNull(StoredFiles.rejectionReason(PDF, true));
        assertNull(StoredFiles.rejectionReason(PNG, true));
    }

    @Test
    void filesOverFiveMegabytesAreRejected() {
        byte[] big = new byte[(int) StoredFiles.MAX_BYTES + 1];
        System.arraycopy(PDF, 0, big, 0, PDF.length);
        assertEquals("The file is larger than 5 MB.", StoredFiles.rejectionReason(big, false));
    }

    @Test
    void fileNamesAreSafeForHeaders() {
        assertEquals("payslip_oct.pdf", StoredFiles.safeFileName("C:\\fakepath\\payslip\"oct.pdf", StoredFiles.PDF));
        assertEquals("evil.html.png", StoredFiles.safeFileName("../../evil.html", StoredFiles.PNG));
        assertEquals("document.pdf", StoredFiles.safeFileName(null, StoredFiles.PDF));
        assertEquals("photo.jpeg", StoredFiles.safeFileName("photo.jpeg", StoredFiles.JPEG));
    }

    @Test
    void checksumIsHex() {
        assertEquals(64, StoredFiles.sha256Hex(PDF).length());
    }
}
