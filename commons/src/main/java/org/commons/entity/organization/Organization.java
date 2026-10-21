package org.commons.entity.organization;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.commons.entity.IdBasedEntity;
import org.commons.enums.OrganizationType;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "organizations")
public class Organization extends IdBasedEntity {

    @Column(length = 15, name = "phone_number")
    protected String phoneNumber;
    @Column(name = "name", length = 245, nullable = false, unique = true)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", length = 50)
    private OrganizationType organizationType;
    @Column(length = 45)
    private String email;
    @ManyToOne
    @JoinColumn(name = "contact_person_id")
    private OrganizationAdmin contactPerson;
    private boolean enabled;
    private boolean deleted;
    // Wrapper types: these columns are nullable, and a primitive float made every
    // query that loaded a record without coordinates fail. Accessors keep the
    // original float signatures (0 when unset) for existing callers.
    private Float longitude;
    private Float latitude;

    @Column(length = 512)
    private String photos;
    private Date registrationDate;
    @Column(name = "remote_file_id")
    private Integer remoteFileId;
    @Column(length = 512)
    private String photoPath;

    @Override
    public String toString() {
        return name + " ~ " + organizationType;
    }

    @Transient
    public String getPhotoPath() {
        if (id == null || photos == null) {
            return "/images/default_image.png";
        }
        return this.photoPath;
    }

    public float getLongitude() {
        return longitude == null ? 0f : longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude == null ? 0f : latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }
}
