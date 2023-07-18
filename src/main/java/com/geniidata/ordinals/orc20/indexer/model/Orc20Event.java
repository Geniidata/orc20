package com.geniidata.ordinals.orc20.indexer.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.geniidata.ordinals.orc20.indexer.enums.EventErrCode;
import com.geniidata.ordinals.orc20.indexer.enums.EventStatus;
import com.geniidata.ordinals.orc20.indexer.enums.EventType;
import com.geniidata.ordinals.orc20.indexer.enums.OP;
import com.geniidata.ordinals.orc20.indexer.utils.Json;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Orc20Event {
    private String eventId; // primary key
    private String tickId;
    private String tick;
    private String inscriptionId;
    private long inscriptionNumber;
    private String fromAddress;
    private String toAddress;
    private EventType eventType;
    private OP op;
    private long nonce;
    private String creator; // only used to track the pending "inscribe-send" and "transfer-send"
    private EventStatus eventStatus;
    private EventErrCode eventErrCode;
    private BigDecimal amount;
    private String extData;
    private String txId;
    private int txIndex;
    private long blockTime;
    private long blockHeight;

    public Orc20Event(String eventId, EventType eventType, OP op, InscriptionTransfer inscriptionTransfer, InscriptionContent inscription, Orc20Metadata orc20Metadata) {
        this.setEventId(eventId);
        this.setOp(op);
        this.setEventType(eventType);

        this.setTick(orc20Metadata.getTick());
        this.setTickId(orc20Metadata.getTickId());

        this.setInscriptionId(inscription.getInscriptionId());
        this.setInscriptionNumber(inscription.getInscriptionNumber());
        this.setExtData(inscription.getContentBody());

        this.setFromAddress(inscriptionTransfer.getFromAddress());
        this.setToAddress(inscriptionTransfer.getToAddress());
        this.setBlockHeight(inscriptionTransfer.getBlockHeight());
        this.setBlockTime(inscriptionTransfer.getBlockTime());
        this.setTxId(inscriptionTransfer.getTxId());
        this.setTxIndex(inscriptionTransfer.getTxIndex());
    }


    @Override
    public String toString() {
        // for dump
        try {
            return Json.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
