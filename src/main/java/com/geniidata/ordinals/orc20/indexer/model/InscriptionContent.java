package com.geniidata.ordinals.orc20.indexer.model;

import lombok.Data;

@Data
public class InscriptionContent {
    private String inscriptionId;
    private long inscriptionNumber;
    private String contentType;
    private String contentBody;
    private long genesisBlockHeight;
}
