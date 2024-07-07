package org.commons.entity.assessment;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;
import org.commons.enums.FormType;
import org.commons.enums.ToolType;

import javax.persistence.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "assessments")
public class Assessment extends IdBasedEntity {
    @Column(length = 128, nullable = false, unique = true)
    private String name;
    @Column(length = 255, nullable = false)
    private String details;
    @Column(name = "assessment_date", length = 245, nullable = false)
    private Date assessmentDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "tool_type", length = 20)
    private ToolType toolType;
    private boolean enabled;
    private boolean deleted;

    @Column(name = "assessment_days")
    private Integer assessmentDays;

    @Transient
    public String getUpdatedTimeOnForm() {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormatter.format(this.assessmentDate);
    }

    public void setUpdatedTimeOnForm(String dateString) {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        try {
            this.assessmentDate = dateFormatter.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return name + " ~ " + getUpdatedTimeOnForm();
    }

}
