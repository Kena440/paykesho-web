package org.commons.entity.announcement;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "announcements")
public class Announcement extends IdBasedEntity {
    @Column(length = 128, nullable = false, unique = true)
    private String name;
    @Column(length = 255, nullable = false)
    private String details;

    private String photos;
    private Date eventDate;

    private boolean enabled;
    private boolean requiresResponse;
    private boolean deleted;

    @Column(name = "event_days")
    private Integer eventDays;

    @Transient
    public String getUpdatedTimeOnForm() {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormatter.format(this.eventDate);
    }

    public void setUpdatedTimeOnForm(String dateString) {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        try {
            this.eventDate = dateFormatter.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Transient
    public String getPhotoPath() {
        if (id == null || photos == null) {
            return "/images/default_image.png";
        }
        return this.photos;
    }

    @Override
    public String toString() {
        return name + " ~ " + getUpdatedTimeOnForm();
    }

}
