package org.paykesho.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import org.paykesho.paymentservice.dto.LencoSettlementRequest;
import org.paykesho.paymentservice.dto.LencoSettlementResponse;
import org.paykesho.paymentservice.service.LencoTransferService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/settlements")
public class LencoSettlementController {

    private final LencoTransferService lencoTransferService;

    @Value("${paykesho.internal.token:}")
    private String internalToken;

    @PostMapping
    public ResponseEntity<?> initiate(
            @RequestHeader(value = "X-PayKesho-Internal-Token", required = false) String suppliedToken,
            @RequestBody LencoSettlementRequest request) {

        if (internalToken == null || internalToken.trim().isEmpty()
                || suppliedToken == null || !internalToken.equals(suppliedToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Unauthorized settlement request."));
        }

        LencoSettlementResponse response = lencoTransferService.initiate(request);
        return ResponseEntity.ok(response);
    }
}
