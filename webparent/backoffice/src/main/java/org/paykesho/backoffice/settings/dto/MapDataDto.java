package org.paykesho.backoffice.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapDataDto {
    private String name;
    private String type;
    private float latitude;
    private float longitude;
}
