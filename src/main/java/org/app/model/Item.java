package org.app.model;

import java.math.BigDecimal;

public class Item {
    private final String description;
    private final String code;
    private final String awb;
    private final BigDecimal kgs;

    public Item(String description, String code, String awb, BigDecimal kgs) {
        this.description = description;
        this.code = code;
        this.awb = awb;
        this.kgs = kgs;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }

    public String getAwb() {
        return awb;
    }

    public BigDecimal getKgs() {
        return kgs;
    }
}
