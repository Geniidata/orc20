package com.geniidata.ordinals.orc20.indexer.data.events;

import java.math.BigDecimal;

import static com.geniidata.ordinals.orc20.indexer.data.NumberValidator.decimalFromString;

/**
 * https://docs.orc20.org/operations#mint-event
 */
public class MintEvent extends BaseEvent {
    private BigDecimal amt;

    public BigDecimal getAmt() {
        return amt;
    }

    public void setAmt(String amt) {
        // validate number
        this.amt = decimalFromString(amt);
    }

    @Override
    public boolean isValid() {
        return super.isValid() && getId() != null
                && getAmt() != null
                && getAmt().compareTo(BigDecimal.ZERO) >= 0;
    }
}
