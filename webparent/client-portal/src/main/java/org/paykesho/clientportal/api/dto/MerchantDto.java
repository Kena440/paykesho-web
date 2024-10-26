package org.paykesho.clientportal.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.commons.enums.MerchantType;

@Data
@AllArgsConstructor
public class MerchantDto {
    private Integer id;
    private String code;
    private String name;
    private MerchantType type;
    private String photos;


}
