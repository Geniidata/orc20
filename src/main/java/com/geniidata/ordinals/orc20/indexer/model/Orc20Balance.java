package com.geniidata.ordinals.orc20.indexer.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.geniidata.ordinals.orc20.indexer.enums.BalanceStatus;
import com.geniidata.ordinals.orc20.indexer.enums.OP;
import com.geniidata.ordinals.orc20.indexer.utils.Json;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Orc20Balance {
    private String tickId; // PrimaryKey
    private String tick;
    private String inscriptionId; // PrimaryKey
    private BigDecimal balance;
    private String address; // PrimaryKey
    private String creator; // only used to track the pending "inscribe-send" and "transfer-send"
    private long nonce;
    private BalanceStatus balanceStatus;
    private OP op;

    private Orc20Balance() {
    }

    // copy
    public Orc20Balance(Orc20Balance orc20Balance) {
        this.setTickId(orc20Balance.getTickId());
        this.setTick(orc20Balance.getTick());
        this.setInscriptionId(orc20Balance.getInscriptionId());
        this.setOp(orc20Balance.getOp());
        this.setAddress(orc20Balance.getAddress());
        this.setCreator(orc20Balance.getCreator());
        this.setNonce(orc20Balance.getNonce());
        this.setBalance(orc20Balance.getBalance());
        this.setBalanceStatus(orc20Balance.getBalanceStatus());
    }

    public Orc20Balance(OP op, InscriptionTransfer inscriptionTransfer, InscriptionContent inscription, Orc20Metadata orc20Metadata) {
        this.setOp(op);

        this.setTick(orc20Metadata.getTick());
        this.setTickId(orc20Metadata.getTickId());

        this.setInscriptionId(inscription.getInscriptionId());
        this.setAddress(inscriptionTransfer.getToAddress());

    }

    /**
     * quickly build "credit balance"
     *
     * @return initial "credit balance"
     */
    public static Orc20Balance createCreditBalance(Orc20Metadata orc20Metadata, String address) {
        Orc20Balance orc20Balance = new Orc20Balance();
        orc20Balance.setOp(OP._VIRTUAL_CREDIT_);
        orc20Balance.setTick(orc20Metadata.getTick());
        orc20Balance.setTickId(orc20Metadata.getTickId());
        orc20Balance.setInscriptionId(""); // no inscription maintains the "credit balance"
        orc20Balance.setAddress(address);
        orc20Balance.setBalance(BigDecimal.ZERO);
        return orc20Balance;
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
