package com.geniidata.ordinals.orc20.indexer.data.events;

import java.math.BigDecimal;

import static com.geniidata.ordinals.orc20.indexer.data.NumberValidator.decimalFromString;
import static com.geniidata.ordinals.orc20.indexer.data.NumberValidator.longFromString;

/**
 * https://docs.orc20.org/operations#send-event
 */
public class SendEvent extends BaseEvent {

    private BigDecimal amt;
    private Long n;

    public BigDecimal getAmt() {
        return amt;
    }

    public void setAmt(String amt) {
        // validate number
        this.amt = decimalFromString(amt);
    }

    public Long getN() {
        return n;
    }

    public void setN(String n) {
        this.n = longFromString(n);
    }

    @Override
    public boolean isValid() {
        return super.isValid() && getId() != null && getN() != null
                && (getAmt() == null || getAmt().compareTo(BigDecimal.ZERO) >= 0);
    }
}
