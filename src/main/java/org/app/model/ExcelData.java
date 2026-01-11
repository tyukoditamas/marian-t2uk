package org.app.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class ExcelData {
    private final List<Item> items;
    private final BigDecimal totalGrossMass;

    public ExcelData(List<Item> items, BigDecimal totalGrossMass) {
        this.items = items == null ? Collections.emptyList() : Collections.unmodifiableList(items);
        this.totalGrossMass = totalGrossMass;
    }

    public List<Item> getItems() {
        return items;
    }

    public BigDecimal getTotalGrossMass() {
        return totalGrossMass;
    }
}
