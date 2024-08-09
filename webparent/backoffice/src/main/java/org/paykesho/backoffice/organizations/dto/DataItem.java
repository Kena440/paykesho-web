package org.paykesho.backoffice.organizations.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
public class DataItem {
    private String identifier;
    private int itemCount;
    //private List<DataMonthItem> monthItems;

    public DataItem(String identifier) {
        this.identifier = identifier;
    }

    public DataItem(String identifier, int itemCount) {
        this.identifier = identifier;
        this.itemCount = itemCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataItem that = (DataItem) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    public void increaseItemCount(int count) {
        this.itemCount += count;
    }

 /*   public void addItem(DataMonthItem item) {
        this.monthItems.add(item);
    }*/
}
