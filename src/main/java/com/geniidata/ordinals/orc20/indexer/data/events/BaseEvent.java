package com.geniidata.ordinals.orc20.indexer.data.events;

public class BaseEvent {
    protected String tick;
    protected String id;
    protected String op;
    protected String p;

    public String getTick() {
        return tick;
    }

    public void setTick(String tick) {
        this.tick = tick;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getP() {
        return p;
    }

    public void setP(String p) {
        this.p = p;
    }

    public boolean isValid() {
        // required
        return getP() != null // required
                &&
                ("orc20".equalsIgnoreCase(getP()) || "orc-20".equalsIgnoreCase(getP())) // only orc20 or orc-20
                &&
                getTick() != null // required
                && getOp() != null; // required
    }
}
