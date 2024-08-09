package org.paykesho.clientportal.organizations.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GPSDataItem {
    private String title;
    private float latitude;
    private float longitude;

    public GPSDataItem(String title, float longitude, float latitude) {
        this.title = title;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
