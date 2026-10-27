package org.paykesho.backoffice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Receives script errors from back-office pages (assets/js/error-handling.js) and
 * writes a short, single-line summary to the log so they show up in monitoring.
 * Only signed-in staff can reach it; nothing is stored.
 */
@Slf4j
@RestController
public class ClientErrorReportController {

    private static final int MAX = 300;

    @PostMapping("/api/errors/report")
    public ResponseEntity<Void> report(@RequestBody(required = false) Map<String, Object> body) {
        if (body != null) {
            log.warn("Browser error on {}: {}", clean(body.get("url")), clean(body.get("error")));
        }
        return ResponseEntity.noContent().build();
    }

    private static String clean(Object value) {
        if (value == null) return "-";
        String s = String.valueOf(value).replaceAll("[\\r\\n\\t]+", " ");
        return s.length() > MAX ? s.substring(0, MAX) + "…" : s;
    }
}
