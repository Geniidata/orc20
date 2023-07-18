package com.geniidata.ordinals.orc20.indexer.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.geniidata.ordinals.orc20.indexer.data.events.DeployEvent;
import com.geniidata.ordinals.orc20.indexer.utils.Json;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Orc20Metadata {
    private String tickId; // primaryKey
    private String tick;
    private String deployId; // "id" before OIP3 , inscription number after OIP3
    private String inscriptionId;
    private long inscriptionNumber;
    private String deployer;
    private long deployTime;
    private BigDecimal max;
    private BigDecimal minted;
    private BigDecimal limit;
    private int decimals;
    private long lastMintTime;
    private boolean upgradeable;
    private String content;
    private long upgradeTime;
    private boolean wrapped; // not support wrapped from brc20 now.

    // build a new tick
    public Orc20Metadata(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription, DeployEvent deployEvent) {
        this.setTickId(inscription.getInscriptionId());
        this.setInscriptionId(inscription.getInscriptionId());
        this.setInscriptionNumber(inscription.getInscriptionNumber());
        this.setContent(inscription.getContentBody());

        this.setTick(deployEvent.getTick());
        this.setMax(deployEvent.getMax());
        this.setLimit(deployEvent.getLim());
        this.setDecimals(deployEvent.getDec());
        this.setUpgradeable(deployEvent.getUg());
        this.setWrapped(deployEvent.getWp());

        this.setDeployer(inscriptionTransfer.getToAddress());
        this.setDeployTime(inscriptionTransfer.getBlockTime());
        this.setLastMintTime(0);
        this.setMinted(BigDecimal.ZERO);
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
