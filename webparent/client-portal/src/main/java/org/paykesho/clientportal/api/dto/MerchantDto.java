package org.paykesho.clientportal.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.commons.constants.Constants;

@Data
@AllArgsConstructor
public class MerchantDto {
    private Integer id;
    private String code;
    private String name;
    private String type;
    private String photos;

    public String getPhotos() {
        return Constants.S3_BASE_URI + "/merchant_logos/" + this.id + "/" + this.photos;
    }
}
