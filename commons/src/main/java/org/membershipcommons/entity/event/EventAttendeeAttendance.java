package org.membershipcommons.entity.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "event_attendee_attendance")
public class EventAttendeeAttendance extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "event_attendee_id")
    private EventAttendeeRegistration eventAttendeeRegistration;
    @Column(name = "event_day")
    private Integer eventDay;
    @Column(name = "signed_date")
    private Date signedDate;
}
