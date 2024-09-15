package org.commons.entity.client;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "client_credit_score")
public class CreditScore extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;
    private int score;
    @Column(name = "score_date")
    private Date scoreDate;
    @Column(name = "last_update")
    private Date lastUpdate;

    public CreditScore(Client client, int score, Date scoreDate) {
        this.client = client;
        this.score = score;
        this.scoreDate = scoreDate;
    }

    @Override
    public String toString() {
        return score + " [" + scoreDate + "]";
    }
}
