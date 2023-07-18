package com.geniidata.ordinals.orc20.indexer.data.events;

import java.math.BigDecimal;

import static com.geniidata.ordinals.orc20.indexer.data.NumberValidator.decimalFromString;
import static com.geniidata.ordinals.orc20.indexer.data.NumberValidator.intFromString;

/**
 * https://docs.orc20.org/operations#upgrade-event
 */
public class UpgradeEvent extends BaseEvent {
    private BigDecimal max;
    private BigDecimal lim;
    private Integer dec;
    private Boolean ug;

    public BigDecimal getMax() {
        return max;
    }

    public void setMax(String max) {
        // validate number
        this.max = decimalFromString(max);
    }

    public BigDecimal getLim() {
        return lim;
    }

    public void setLim(String lim) {
        // validate number
        this.lim = decimalFromString(lim);
    }

    public Integer getDec() {
        return dec;
    }

    public void setDec(String dec) {
        // validate number
        this.dec = intFromString(dec);
    }

    public Boolean getUg() {
        return ug;
    }

    public void setUg(Boolean ug) {
        this.ug = ug;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && getId() != null;
    }
}
