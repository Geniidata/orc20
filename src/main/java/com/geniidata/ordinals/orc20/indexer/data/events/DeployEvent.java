package com.geniidata.ordinals.orc20.indexer.data.events;

import com.geniidata.ordinals.orc20.indexer.contants.OIP;

import java.math.BigDecimal;

import static com.geniidata.ordinals.orc20.indexer.data.NumberValidator.decimalFromString;
import static com.geniidata.ordinals.orc20.indexer.data.NumberValidator.intFromString;

/**
 * https://docs.orc20.org/operations#deploy-or-migrate-event
 */
public class DeployEvent extends BaseEvent {
    private Boolean ug = OIP.DEPLOY_UPGRADABLE_DEFAULT;
    private Boolean wp = OIP.DEPLOY_WP_DEFAULT;
    private BigDecimal max = OIP.DEPLOY_MAX_UINT256;
    private BigDecimal lim = OIP.DEPLOY_MIN_DEFAULT;
    private Integer dec = OIP.DEPLOY_DEC_DEFAULT;

    public Boolean getUg() {
        return ug;
    }

    public void setUg(Boolean ug) {
        this.ug = ug;
    }

    public Boolean getWp() {
        return wp;
    }

    public void setWp(Boolean wp) {
        this.wp = wp;
    }

    public BigDecimal getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = decimalFromString(max);
    }

    public BigDecimal getLim() {
        return lim;
    }

    public void setLim(String lim) {
        this.lim = decimalFromString(lim);
    }

    public Integer getDec() {
        return dec;
    }

    public void setDec(String dec) {
        this.dec = intFromString(dec);
    }

    @Override
    public boolean isValid() {
        return super.isValid()
                && (getDec() == null || getDec() >= 0 && getDec() <= OIP.DEPLOY_DEC_MAX)
                && getMax().compareTo(OIP.DEPLOY_MAX_UINT256) <= 0;
    }
}
