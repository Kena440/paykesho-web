package org.commons.entity.settings;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@Entity
@Table(name = "funding_projects")
public class FundingProject extends IdBasedEntity {
    @Column(length = 40, nullable = false, unique = true)
    private String projectName;
    @Column(length = 140, nullable = false)
    private String projectCode;
    private boolean deleted;

    public FundingProject(String projectName, String projectCode) {
        this.projectName = projectName;
        this.projectCode = projectCode;
    }

    public FundingProject(String projectName) {
        this.projectName = projectName;
    }

    public FundingProject(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.projectName;
    }
}
