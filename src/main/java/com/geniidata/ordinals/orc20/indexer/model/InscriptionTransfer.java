package com.geniidata.ordinals.orc20.indexer.model;

import lombok.Data;

@Data
public class InscriptionTransfer {
    private String inscriptionId;
    private long inscriptionNumber;
    private String fromAddress;
    private String toAddress;
    private String toLocation;
    private long blockHeight;
    private long blockTime;
    private String txId;
    private int txIndex;
    private boolean isTransfer; // create an inscription or transfer an inscription

}
