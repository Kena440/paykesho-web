package org.paykesho.clientportal.api.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
public class VoucherRequestDto {
    private BigDecimal amount;
    private String reason;
    private MultipartFile payslip;
}
