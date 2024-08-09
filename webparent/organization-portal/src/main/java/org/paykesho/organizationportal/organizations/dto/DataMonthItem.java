package org.paykesho.organizationportal.organizations.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
public class DataMonthItem {
    private String year;
    private String identifier;
    private int itemCount;

    public DataMonthItem(String identifier) {
        this.identifier = identifier;
    }

    public DataMonthItem(String year, String identifier, int itemCount) {
        this.year = year;
        this.identifier = identifier;
        this.itemCount = itemCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataMonthItem that = (DataMonthItem) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    public void increaseItemCount(int count) {
        this.itemCount += count;
    }
}
