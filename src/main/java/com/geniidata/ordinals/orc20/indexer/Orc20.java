package com.geniidata.ordinals.orc20.indexer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.geniidata.ordinals.orc20.indexer.contants.OIP;
import com.geniidata.ordinals.orc20.indexer.data.NumberValidator;
import com.geniidata.ordinals.orc20.indexer.data.events.*;
import com.geniidata.ordinals.orc20.indexer.enums.*;
import com.geniidata.ordinals.orc20.indexer.model.*;
import com.geniidata.ordinals.orc20.indexer.storage.MemoryCache;
import com.geniidata.ordinals.orc20.indexer.utils.Json;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Orc20 {
    private final static Logger logger = Logger.getLogger("orc20");

    static {
        logger.setLevel(Level.WARNING);
    }

    // when the block height of OIP10 is reached, create a balance snapshot
    private boolean isOIP10BackedUp = false;

    /**
     * try to decode json to BaseEvent, check the required fields
     */
    public static BaseEvent isOrc20(String content) {

        BaseEvent baseEvent = readInscriptionContent(content, BaseEvent.class);
        if (baseEvent != null && baseEvent.isValid()) {
            return baseEvent;
        }
        return null;
    }

    /**
     * json decode
     * convert content to lowercase
     */
    public static <T> T readInscriptionContent(String content, Class<T> cls) {
        if (content == null) {
            return null;
        }
        T event = null;
        try {
            event = Json.readValue(content.toLowerCase(), cls); //  "All ORC-20 data are case-insensitive." https://docs.orc20.org/#concept-of-orc-20
        } catch (JsonProcessingException | NumberValidator.DecimalsValidatorException ignored) {
        }
        return event;
    }

    /**
     * deal with inscription creation/transfer
     */
    public void accept(InscriptionTransfer inscriptionTransfer) {

        long blockHeight = inscriptionTransfer.getBlockHeight();
        if (!isOIP10BackedUp && !OIP.beforeOIP10(blockHeight)) {
            MemoryCache.createBalanceSnapshot();
            logger.info("oip10 balance snapshot created.");
//            Dumper.dumpOrc20BalanceOIP10Snapshot();
            isOIP10BackedUp = true;
        }

        String inscriptionId = inscriptionTransfer.getInscriptionId();
        InscriptionContent inscription = MemoryCache.selectInscriptionContentByInscriptionId(inscriptionId);
        if (inscription == null) {
            logger.warning("Lost inscription content: " + inscriptionId);
            return;
        }
        String eventId = getEventId(inscriptionTransfer);
        if (eventProcessed(eventId)) {
            logger.info("Event has been processed: " + eventId);
            return;
        }
        if (inscriptionTransfer.isTransfer()) {
            transfer(inscriptionTransfer, inscription);
        } else {
            inscribe(inscriptionTransfer, inscription);
        }
    }

    /**
     * inscribe an orc20 inscription
     */
    private void inscribe(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription) {
        String content = inscription.getContentBody();
        String inscriptionId = inscription.getInscriptionId();
        BaseEvent baseEvent = isOrc20(content);
        if (baseEvent == null || !baseEvent.isValid()) {
            logger.info("Not a valid ORC20 inscription: " + inscriptionId);
        } else {
            String op = baseEvent.getOp();
            if ("deploy".equals(op)) {
                inscribeDeploy(inscriptionTransfer, inscription);
            } else {
                String tick = baseEvent.getTick();
                String id = baseEvent.getId();
                long blockHeight = inscription.getGenesisBlockHeight();
                Orc20Metadata orc20Metadata = getMetadata(tick, id, blockHeight);
                if (orc20Metadata == null) {
                    logger.info("No tick matches " + inscriptionId);
                } else {
                    switch (op) {
                        case "mint":
                            inscribeMint(inscriptionTransfer, inscription, orc20Metadata);
                            break;
                        case "send":
                        case "transfer":
                            inscribeSend(inscriptionTransfer, inscription, orc20Metadata); // "Use send or transfer compatible with BRC-20 OIP-2" https://docs.orc20.org/operations#send-event
                            break;
                        case "cancel":
                            inscribeCancel(inscriptionTransfer, inscription, orc20Metadata);
                            break;
                        case "list":
                            // not support
                            break;
                        case "upgrade":
                            inscribeUpgrade(inscriptionTransfer, inscription, orc20Metadata);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    /**
     * transfer an orc20 inscription
     */
    private void transfer(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription) {
        String content = inscription.getContentBody();
        String inscriptionId = inscription.getInscriptionId();
        BaseEvent baseEvent = isOrc20(content);
        if (baseEvent == null || !baseEvent.isValid()) {
            logger.info("Not an ORC20 inscription: " + inscriptionId);
        } else {
            String op = baseEvent.getOp();
            String tick = baseEvent.getTick();
            switch (op) {
                case "deploy":
                    transferDeploy(inscriptionTransfer, inscription, tick);
                    break;
                case "mint":
                    transferMint(inscriptionTransfer, inscription);
                    break;
                case "send":
                case "transfer":
                    transferSend(inscriptionTransfer, inscription);
                    break;
                case "cancel":
                    // do nothing;
                    break;
                case "list":
                    // not support
                    break;
                case "upgrade":
                    transferUpgrade(inscriptionTransfer, inscription);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * inscribe deploy: deploy an orc20 tick
     */
    private void inscribeDeploy(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription) {
        DeployEvent deployEvent = readInscriptionContent(inscription.getContentBody(), DeployEvent.class);
        String inscriptionId = inscription.getInscriptionId();
        if (deployEvent == null || !deployEvent.isValid()) {
            logger.info("Invalid ORC20 deploy inscription:" + inscriptionId);
            return;
        }

        // validate dec
        int dec = deployEvent.getDec();
        if (exceedingDecimal(deployEvent.getMax(), dec, null)) {
            return;
        }
        if (exceedingDecimal(deployEvent.getLim(), dec, null)) {
            return;
        }

        String tick = deployEvent.getTick();
        String id = deployEvent.getId();

        Orc20Metadata metadata = new Orc20Metadata(inscriptionTransfer, inscription, deployEvent);
        Orc20Event orc20Event = new Orc20Event(getEventId(inscriptionTransfer), EventType.INSCRIBE_DEPLOY, OP.DEPLOY, inscriptionTransfer, inscription, metadata);

        orc20Event.setAmount(null);
        orc20Event.setNonce(0); // nonsense for deploy

        if (!OIP.beforeOIP3(inscriptionTransfer.getBlockHeight())) {
            // after OIP-3, ignore 'id' in json, and use "deployment inscription number" as 'deployId'
            metadata.setDeployId(String.valueOf(inscription.getInscriptionNumber()));
            MemoryCache.insertOrc20Metadata(metadata);
            orc20Event.setEventStatus(EventStatus.SUCCESS);
        } else {
            // before OIP-3, use the 'id' field in JSON as 'deployId'
            if (MemoryCache.selectORC20MetadataByTickAndDeployId(tick, id) != null) {
                // redeployment with same tick+id
                orc20Event.setEventStatus(EventStatus.FAILED);
                orc20Event.setEventErrCode(EventErrCode.REDEPLOYMENT);
            } else {
                metadata.setDeployId(id); // may be null
                MemoryCache.insertOrc20Metadata(metadata);
                orc20Event.setEventStatus(EventStatus.SUCCESS);
            }
        }
        MemoryCache.insertOrc20Event(orc20Event);
    }

    /**
     * inscribe mint to minter's address
     */
    private void inscribeMint(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription, Orc20Metadata orc20Metadata) {
        Orc20Event orc20Event = new Orc20Event(getEventId(inscriptionTransfer), EventType.INSCRIBE_MINT, OP.MINT, inscriptionTransfer, inscription, orc20Metadata);
        MintEvent mintEvent = readInscriptionContent(inscription.getContentBody(), MintEvent.class);
        if (!isValidEvent(mintEvent, orc20Event)) {
            return;
        }
        // validate dec
        if (exceedingDecimal(mintEvent.getAmt(), orc20Metadata.getDecimals(), orc20Event)) {
            return;
        }
        BigDecimal mintAmount = mintEvent.getAmt();
        BigDecimal lim = orc20Metadata.getLimit();

        orc20Event.setAmount(mintAmount);
        Orc20Balance orc20Balance = new Orc20Balance(OP.MINT, inscriptionTransfer, inscription, orc20Metadata);
        orc20Balance.setNonce(0); // nonsense for mint

        if (mintAmount.compareTo(lim) > 0) {
            orc20Event.setEventStatus(EventStatus.FAILED);
            orc20Event.setEventErrCode(EventErrCode.EXCEEDING_LIMIT);

            orc20Balance.setBalanceStatus(BalanceStatus.INVALID);
        } else {
            BigDecimal minted = orc20Metadata.getMinted();
            if (minted == null) {
                minted = BigDecimal.ZERO;
            }
            minted = minted.add(mintAmount);
            BigDecimal max = orc20Metadata.getMax();
            if (minted.compareTo(max) > 0) {
                orc20Event.setEventStatus(EventStatus.FAILED);
                orc20Event.setEventErrCode(EventErrCode.EXCEEDING_SUPPLY);

                orc20Balance.setBalanceStatus(BalanceStatus.INVALID);
            } else {
                orc20Metadata.setMinted(minted);
                orc20Metadata.setLastMintTime(inscriptionTransfer.getBlockTime());
                MemoryCache.insertOrc20Metadata(orc20Metadata);

                orc20Balance.setBalance(mintAmount);
                orc20Balance.setBalanceStatus(BalanceStatus.OK);

                orc20Event.setEventStatus(EventStatus.SUCCESS);
            }
        }
        MemoryCache.insertOrc20Event(orc20Event);
        MemoryCache.insertOrc20Balance(orc20Balance);
    }

    /**
     * after OIP10, 'inscribe-send' means withdrawing from the 'ATM'.
     */
    private void _withdraw(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription, Orc20Metadata orc20Metadata) {
        Orc20Event orc20Event = new Orc20Event(getEventId(inscriptionTransfer), EventType.WITHDRAW, OP.SEND, inscriptionTransfer, inscription, orc20Metadata);
        SendEvent sendEvent = readInscriptionContent(inscription.getContentBody(), SendEvent.class);
        if (!isValidEvent(sendEvent, orc20Event)) {
            return;
        }
        // validate dec
        if (exceedingDecimal(sendEvent.getAmt(), orc20Metadata.getDecimals(), orc20Event)) {
            return;
        }
        BigDecimal sendAmount = sendEvent.getAmt();
        Orc20Balance withdrawBalance = new Orc20Balance(OP.SEND, inscriptionTransfer, inscription, orc20Metadata);
        if (sendAmount == null) {
            // after oip10, remaining-balance is invalid
            withdrawBalance.setBalanceStatus(BalanceStatus.INVALID);
            orc20Event.setEventStatus(EventStatus.FAILED);
            orc20Event.setEventErrCode(EventErrCode.INVALID_INSCRIPTION);
        } else {
            String tickId = orc20Metadata.getTickId();
            String address = inscriptionTransfer.getToAddress();
            // get current credit balance
            List<Orc20Balance> balanceList = MemoryCache.selectOrc20BalanceListByTickIdAndAddress(tickId, address);
            Orc20Balance creditBalance = null;
            if (balanceList != null && balanceList.size() > 0) {
                for (Orc20Balance balance : balanceList) {
                    if (OP._VIRTUAL_CREDIT_.equals(balance.getOp()) && BalanceStatus.OK.equals(balance.getBalanceStatus())) {
                        creditBalance = balance;
                        break;
                    }
                }
            }
            if (creditBalance == null) {
                creditBalance = Orc20Balance.createCreditBalance(orc20Metadata, address);
            }
            if (creditBalance.getBalance().compareTo(sendAmount) >= 0) {
                // withdraw successfully
                withdrawBalance.setBalance(sendAmount);
                withdrawBalance.setBalanceStatus(BalanceStatus.OK);

                creditBalance.setBalanceStatus(BalanceStatus.OK);
                creditBalance.setBalance(creditBalance.getBalance().subtract(sendAmount));
                MemoryCache.insertOrc20Balance(creditBalance);

                orc20Event.setAmount(sendAmount);
                orc20Event.setEventStatus(EventStatus.SUCCESS);
            } else {
                // insufficient credit balance
                withdrawBalance.setBalanceStatus(BalanceStatus.INVALID);
                orc20Event.setAmount(sendAmount);
                orc20Event.setEventStatus(EventStatus.FAILED);
                orc20Event.setEventErrCode(EventErrCode.INSUFFICIENT_BALANCE);
            }
        }
        MemoryCache.insertOrc20Balance(withdrawBalance);
        MemoryCache.insertOrc20Event(orc20Event);
    }

    /**
     * 'inscribe send' before OIP10
     */
    private void _inscribeSend(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription, Orc20Metadata orc20Metadata) {
        Orc20Event orc20Event = new Orc20Event(getEventId(inscriptionTransfer), EventType.INSCRIBE_SEND, OP.SEND, inscriptionTransfer, inscription, orc20Metadata);
        SendEvent sendEvent = readInscriptionContent(inscription.getContentBody(), SendEvent.class);
        if (!isValidEvent(sendEvent, orc20Event)) {
            return;
        }
        // validate dec
        if (exceedingDecimal(sendEvent.getAmt(), orc20Metadata.getDecimals(), orc20Event)) {
            return;
        }

        BigDecimal sendAmount = sendEvent.getAmt();
        orc20Event.setAmount(sendAmount);

        String address = inscriptionTransfer.getToAddress();
        Long nonce = sendEvent.getN();
        if (nonce == null) {
            // "n" is required before oip10
            orc20Event.setEventStatus(EventStatus.FAILED);
            orc20Event.setEventErrCode(EventErrCode.INVALID_INSCRIPTION);
            MemoryCache.insertOrc20Event(orc20Event);
            return;
        }
        orc20Event.setNonce(nonce);
        orc20Event.setCreator(address); // creator is used for tracing the pending "inscribe send" which may be transferred before "remaining balance"
        OP op = OP.SEND;
        if (sendAmount == null) {
            op = OP.REMAINING_BALANCE;
        }
        orc20Event.setOp(op);
        Orc20Balance orc20Balance = new Orc20Balance(op, inscriptionTransfer, inscription, orc20Metadata);
        orc20Balance.setCreator(address);
        String tickId = orc20Metadata.getTickId();
        List<Orc20Balance> currentBalanceList = MemoryCache.selectOrc20BalanceListByTickIdAndAddress(tickId, address);
        List<Orc20Balance> currentOkBalanceList = currentBalanceList.stream().filter(b -> b.getBalanceStatus().equals(BalanceStatus.OK)).collect(Collectors.toList());
        List<Orc20Balance> currentPendingBalanceList = MemoryCache.selectPendingOrc20BalanceListByTickIdAndCreator(tickId, address);
        BigDecimal totalBalance = BigDecimal.ZERO;
        BigDecimal sendingBalance = BigDecimal.ZERO;
        for (Orc20Balance b : currentOkBalanceList) {
            totalBalance = totalBalance.add(b.getBalance());
        }
        for (Orc20Balance b : currentPendingBalanceList) {
            long pendingNonce = b.getNonce();
            if (nonce == pendingNonce) {
                // duplicated "n"
                orc20Event.setNonce(nonce);
                orc20Event.setEventStatus(EventStatus.FAILED);
                orc20Event.setEventErrCode(EventErrCode.DUPLICATED_NONCE);
                MemoryCache.insertOrc20Event(orc20Event);

                orc20Balance.setNonce(nonce);
                orc20Balance.setBalanceStatus(BalanceStatus.INVALID);
                MemoryCache.insertOrc20Balance(orc20Balance);
                return;
            } else {
                sendingBalance = sendingBalance.add(b.getBalance());
            }
        }

        if (sendAmount == null) {
            // remaining balance
            orc20Event.setOp(OP.REMAINING_BALANCE);
            orc20Event.setEventType(EventType.INSCRIBE_REMAINING_BALANCE);
            if (currentPendingBalanceList.size() > 0) {
                BigDecimal remainingBalance = totalBalance.subtract(sendingBalance);
                if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                    // insufficient balance, all the pending "inscribe send" balances are marked invalid
                    currentPendingBalanceList.forEach(
                            b -> {
                                b.setBalanceStatus(BalanceStatus.INVALID); // update balance status
                            }
                    );
                    MemoryCache.updateOrc20EventStatusByTickIdAndCreator(tickId, address, EventStatus.SEND_PENDING, EventStatus.FAILED); // failed all the pending "inscribe-send" events
                    orc20Event.setEventStatus(EventStatus.FAILED);
                    orc20Event.setEventErrCode(EventErrCode.INSUFFICIENT_BALANCE);

                    orc20Balance.setBalanceStatus(BalanceStatus.INVALID);
                } else {
                    // success
                    // 1. The inscriptions which maintain the sender's balance are invalidated.
                    currentOkBalanceList.forEach(
                            b -> {
                                b.setBalanceStatus(BalanceStatus.EXPIRED); // update balance status
                            }
                    );
                    // 2. After the completion of a transaction, "inscribe send" inscriptions in all partial transactions maintain new balance
                    currentPendingBalanceList.forEach(
                            b -> {
                                b.setBalanceStatus(BalanceStatus.OK); // update balance status
                            }
                    );
                    // 3. The "remaining balance" inscription maintains the remaining balance
                    orc20Balance.setBalanceStatus(BalanceStatus.OK);
                    orc20Balance.setBalance(remainingBalance);
                    // 4. updates the status of the "inscribe send" event to success
                    MemoryCache.updateOrc20EventStatusByTickIdAndCreator(tickId, address, EventStatus.SEND_PENDING, EventStatus.SUCCESS);
                    // 5. "remaining balance" event
                    orc20Event.setEventStatus(EventStatus.SUCCESS);
                }
            } else {
                // missing "inscribe send"
                orc20Event.setEventStatus(EventStatus.FAILED);
                orc20Event.setEventErrCode(EventErrCode.MISSING_INSCRIBE_SEND);
                orc20Balance.setBalanceStatus(BalanceStatus.INVALID);
            }
        } else {
            // inscribe-send
            orc20Balance.setBalance(sendAmount);
            orc20Balance.setNonce(nonce);
            orc20Balance.setBalanceStatus(BalanceStatus.SEND_PENDING); // wait for "remaining balance"

            orc20Event.setAmount(sendAmount);
            orc20Event.setEventStatus(EventStatus.SEND_PENDING); // wait for "remaining balance"
        }
        MemoryCache.insertOrc20Balance(orc20Balance);
        MemoryCache.insertOrc20Event(orc20Event);

    }

    /**
     * if the blockHeight is before OIP-10, this function routes to '_inscribeSend', otherwise it routes to '_withdraw'.
     */
    private void inscribeSend(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription, Orc20Metadata orc20Metadata) {
        long blockHeight = inscriptionTransfer.getBlockHeight();
        if (OIP.beforeOIP10(blockHeight)) {
            _inscribeSend(inscriptionTransfer, inscription, orc20Metadata);
        } else {
            _withdraw(inscriptionTransfer, inscription, orc20Metadata);
        }
    }

    /**
     * 'inscribe cancel': cancel partial transactions before the final step of the send operation.
     */
    private void inscribeCancel(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription, Orc20Metadata orc20Metadata) {
        CancelEvent cancelEvent = readInscriptionContent(inscription.getContentBody(), CancelEvent.class);
        Orc20Event orc20Event = new Orc20Event(getEventId(inscriptionTransfer), EventType.INSCRIBE_CANCEL, OP.CANCEL, inscriptionTransfer, inscription, orc20Metadata);
        if (!isValidEvent(cancelEvent, orc20Event)) {
            return;
        }
        long blockHeight = inscriptionTransfer.getBlockHeight();
        if (OIP.beforeOIP10(blockHeight)) {
            String address = inscriptionTransfer.getToAddress();
            List<Long> nonceList = cancelEvent.getN();
            if (nonceList.size() > 0) {
                String tickId = orc20Metadata.getTickId();
                for (long nonce : nonceList) {
                    MemoryCache.updateOrc20BalanceStatusByTickIdAndCreatorAndNonce(tickId, address, nonce, BalanceStatus.CANCELED);
                    MemoryCache.updateOrc20EventStatusByTickIdAndCreatorAndNonce(tickId, address, nonce, EventStatus.CANCELED);
                }
            }
            orc20Event.setEventStatus(EventStatus.SUCCESS);
        } else {
            orc20Event.setEventStatus(EventStatus.FAILED);
            orc20Event.setEventErrCode(EventErrCode.INVALID_INSCRIPTION);
        }
        MemoryCache.insertOrc20Event(orc20Event);
    }

    /**
     * 'inscribe-upgrade': step 1 of upgrade
     */
    private void inscribeUpgrade(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription, Orc20Metadata orc20Metadata) {
        UpgradeEvent upgradeEvent = readInscriptionContent(inscription.getContentBody(), UpgradeEvent.class);
        Orc20Event orc20Event = new Orc20Event(getEventId(inscriptionTransfer), EventType.INSCRIBE_UPGRADE, OP.UPGRADE, inscriptionTransfer, inscription, orc20Metadata);
        if (!isValidEvent(upgradeEvent, orc20Event)) {
            return;
        }

        // validate dec
        int dec = upgradeEvent.getDec() == null ? orc20Metadata.getDecimals() : upgradeEvent.getDec();
        BigDecimal max = upgradeEvent.getMax() == null ? orc20Metadata.getMax() : upgradeEvent.getMax();
        if (exceedingDecimal(max, dec, orc20Event)) {
            return;
        }
        BigDecimal lim = upgradeEvent.getLim() == null ? orc20Metadata.getLimit() : upgradeEvent.getLim();
        if (exceedingDecimal(lim, dec, orc20Event)) {
            return;
        }

        String receiver = inscriptionTransfer.getToAddress();
        String deployer = orc20Metadata.getDeployer();
        boolean upgradeable = orc20Metadata.isUpgradeable();
        if (upgradeable) {
            if (receiver.equals(deployer)) {
                // deployer inscribe an upgrade inscription
                orc20Event.setEventStatus(EventStatus.UPGRADE_WAITING);
            } else {
                orc20Event.setEventStatus(EventStatus.UPGRADE_BLOCKED); // if the deployer transfers 'deploy inscription' to receiver, the upgrade may finally succeed.
            }
        } else {
            // Non-upgradeable
            orc20Event.setEventStatus(EventStatus.FAILED);
            orc20Event.setEventErrCode(EventErrCode.NON_UPGRADEABLE);
        }
        MemoryCache.insertOrc20Event(orc20Event);
    }

    /**
     * transfer deploy: change deployer
     */
    private void transferDeploy(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription, String tick) {
        long inscriptionNumber = inscription.getInscriptionNumber(); // deployment inscription number
        Orc20Metadata orc20Metadata = MemoryCache.selectORC20MetadataByTickAndInscriptionNumber(tick, inscriptionNumber);
        if (orc20Metadata == null) {
            logger.info("Undeployed tick: " + inscriptionNumber);
            return;
        }
        Orc20Event orc20Event = new Orc20Event(getEventId(inscriptionTransfer), EventType.TRANSFER_DEPLOY, OP.DEPLOY, inscriptionTransfer, inscription, orc20Metadata);
        String tickId = orc20Metadata.getTickId();
        String newDeployer = inscriptionTransfer.getToAddress();
        String oldDeployer = orc20Metadata.getDeployer();
        orc20Metadata.setDeployer(newDeployer); // change the deployer
        MemoryCache.updateOrc20EventStatusByTickIdAndToAddress(tickId, newDeployer, EventStatus.UPGRADE_BLOCKED, EventStatus.UPGRADE_WAITING, null);
        MemoryCache.updateOrc20EventStatusByTickIdAndToAddress(tickId, oldDeployer, EventStatus.UPGRADE_WAITING, EventStatus.UPGRADE_BLOCKED, null);
        orc20Event.setEventStatus(EventStatus.SUCCESS);
        MemoryCache.insertOrc20Event(orc20Event);
    }

    /**
     * transfer mint(reusing "mint inscription")
     */
    private void _transferMint(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription) {
        String inscriptionId = inscription.getInscriptionId();
        Orc20Balance balance = MemoryCache.selectOrc20BalanceByInscriptionId(inscriptionId);
        if (balance == null) {
            logger.info("Inscription that does not maintain balance:" + inscriptionId);
            return;
        }
        String tickId = balance.getTickId();
        Orc20Metadata orc20Metadata = MemoryCache.selectORC20MetadataByTickId(tickId);
        Orc20Event orc20Event = new Orc20Event(getEventId(inscriptionTransfer), EventType.TRANSFER_MINT, OP.MINT, inscriptionTransfer, inscription, orc20Metadata);
        String receiver = inscriptionTransfer.getToAddress();
        String sender = inscriptionTransfer.getFromAddress();
        if (BalanceStatus.OK.equals(balance.getBalanceStatus())) {
            orc20Event.setAmount(balance.getBalance());
            orc20Event.setEventStatus(EventStatus.SUCCESS);
        } else {
            orc20Event.setEventStatus(EventStatus.FAILED);
            orc20Event.setEventErrCode(EventErrCode.INEFFECTIVE_INSCRIPTION);
        }
        MemoryCache.insertOrc20Event(orc20Event);
        MemoryCache.updateOrc20BalanceAddressByTickIdAndInscriptionId(tickId, inscriptionId, sender, receiver);
    }

    /**
     * transfer balance to ATM
     */
    private void _deposit(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription) {
        String inscriptionId = inscription.getInscriptionId();
        Orc20Balance balance = MemoryCache.selectOrc20BalanceByInscriptionId(inscriptionId);
        if (balance == null) {
            logger.info("Transferred an inscription that does not maintain balance:" + inscriptionId);
            return;
        }
        String tickId = balance.getTickId();
        Orc20Metadata orc20Metadata = MemoryCache.selectORC20MetadataByTickId(tickId);
        OP op = balance.getOp();
        Orc20Event orc20Event = new Orc20Event(getEventId(inscriptionTransfer), EventType.DEPOSIT, op, inscriptionTransfer, inscription, orc20Metadata);
        String sender = inscriptionTransfer.getFromAddress();
        if (BalanceStatus.OK.equals(balance.getBalanceStatus())) {
            if (OP.SHADOW_REMAINING_BALANCE.equals(op)) {
                // remaining balance locked after transfer to "Non-ATM"
                // should not happen
                orc20Event.setEventStatus(EventStatus.FAILED);
                orc20Event.setEventErrCode(EventErrCode.INEFFECTIVE_INSCRIPTION);
            } else {
                BigDecimal depositAmount = balance.getBalance();
                orc20Event.setAmount(depositAmount);
                orc20Event.setEventStatus(EventStatus.SUCCESS);

                balance.setBalanceStatus(BalanceStatus.EXPIRED); // marks the inscription invalid
                List<Orc20Balance> currentBalanceList = MemoryCache.selectOrc20BalanceListByTickIdAndAddress(tickId, sender);
                Orc20Balance creditBalance = null;
                if (currentBalanceList != null && currentBalanceList.size() > 0) {
                    for (Orc20Balance currentBalance : currentBalanceList) {
                        if (OP._VIRTUAL_CREDIT_.equals(currentBalance.getOp()) && BalanceStatus.OK.equals(currentBalance.getBalanceStatus())) {
                            creditBalance = currentBalance;
                            break;
                        }
                    }
                }
                if (creditBalance == null) {
                    creditBalance = Orc20Balance.createCreditBalance(orc20Metadata, sender);
                    creditBalance.setBalanceStatus(BalanceStatus.OK);
                }
                creditBalance.setBalance(creditBalance.getBalance().add(depositAmount));
                MemoryCache.insertOrc20Balance(creditBalance); // update credit balance
            }
        } else {
            orc20Event.setEventStatus(EventStatus.FAILED);
            orc20Event.setEventErrCode(EventErrCode.INEFFECTIVE_INSCRIPTION);
        }
        MemoryCache.insertOrc20Event(orc20Event);
    }

    /**
     * if the blockHeight is before OIP-10 and the receiver is "ATM", this function routes to '_transferMint', otherwise it routes to '_deposit'.
     */
    private void transferMint(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription) {
        long blockHeight = inscriptionTransfer.getBlockHeight();
        String receiver = inscriptionTransfer.getToAddress();
        if (!OIP.beforeOIP10(blockHeight) && OIP.isVirtualATMAddress(receiver)) {
            _deposit(inscriptionTransfer, inscription);
        } else {
            _transferMint(inscriptionTransfer, inscription);
        }
    }

    /**
     * transfer send/remaining-balance(reusing "send inscription")
     */
    private void _transferSend(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription) {
        String inscriptionId = inscription.getInscriptionId();
        Orc20Balance balance = MemoryCache.selectOrc20BalanceByInscriptionId(inscriptionId);
        if (balance == null) {
            logger.info("Transferred an inscription that does not maintain balance:" + inscriptionId);
            return;
        }
        String tickId = balance.getTickId();
        Orc20Metadata orc20Metadata = MemoryCache.selectORC20MetadataByTickId(tickId);
        OP op = balance.getOp();
        String eventId = getEventId(inscriptionTransfer);
        Orc20Event orc20Event = new Orc20Event(eventId, EventType.TRANSFER_SEND, op, inscriptionTransfer, inscription, orc20Metadata);
        String receiver = inscriptionTransfer.getToAddress();
        String sender = inscriptionTransfer.getFromAddress();
        if (BalanceStatus.OK.equals(balance.getBalanceStatus())) {
            long blockHeight = inscriptionTransfer.getBlockHeight();
            // Before OIP10, both 'inscribe-send' and 'inscribe-remaining-balance' inscriptions were transferable.
            // After OIP10, only 'inscribe-send' inscription can be transferred.
            if (OIP.beforeOIP10(blockHeight) || OP.SEND.equals(op)) {
                BigDecimal sendAmount = balance.getBalance();
                orc20Event.setAmount(sendAmount);
                orc20Event.setEventStatus(EventStatus.SUCCESS);
                MemoryCache.updateOrc20BalanceAddressByTickIdAndInscriptionId(tickId, inscriptionId, sender, receiver); // change balance holder
            } else {
                // after oip10 and op = 'remaining-balance'
                orc20Event.setEventStatus(EventStatus.FAILED);
                if (OP.REMAINING_BALANCE.equals(op)) {
                    orc20Event.setEventErrCode(EventErrCode.REMAINING_BALANCE_LOCKED);
                    balance.setOp(OP.SHADOW_REMAINING_BALANCE); // use a special op to lock the "remaining balance"
                } else {
                    // SHADOW_REMAINING_BALANCE
                    orc20Event.setEventErrCode(EventErrCode.INEFFECTIVE_INSCRIPTION);
                }
            }
        } else if (BalanceStatus.SEND_PENDING.equals(balance.getBalanceStatus())) {
            // transfer an "inscribe-send" before remaining-balance
            BigDecimal sendAmount = balance.getBalance();
            orc20Event.setAmount(sendAmount);
            orc20Event.setEventStatus(EventStatus.SEND_PENDING);
            orc20Event.setCreator(balance.getCreator());
            orc20Event.setNonce(balance.getNonce());
            MemoryCache.updateOrc20BalanceAddressByTickIdAndInscriptionId(tickId, inscriptionId, sender, receiver); // change balance holder
        } else {
            // FAILED
            orc20Event.setEventStatus(EventStatus.FAILED);
            orc20Event.setEventErrCode(EventErrCode.INEFFECTIVE_INSCRIPTION);
            MemoryCache.updateOrc20BalanceAddressByTickIdAndInscriptionId(tickId, inscriptionId, sender, receiver); // change balance holder
        }
        MemoryCache.insertOrc20Event(orc20Event);
    }

    /**
     * 'transfer send' router: before/after oip10
     */
    private void transferSend(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription) {
        long blockHeight = inscriptionTransfer.getBlockHeight();
        String receiver = inscriptionTransfer.getToAddress();
        if (!OIP.beforeOIP10(blockHeight) && OIP.isVirtualATMAddress(receiver)) {
            _deposit(inscriptionTransfer, inscription);
        } else {
            _transferSend(inscriptionTransfer, inscription);
        }
    }

    /**
     * transfer upgrade: step 2 of upgrade
     */
    private void transferUpgrade(InscriptionTransfer inscriptionTransfer, InscriptionContent inscription) {
        String inscriptionId = inscription.getInscriptionId();
        String sender = inscriptionTransfer.getFromAddress();
        String receiver = inscriptionTransfer.getToAddress();
        String inscriptionContent = inscription.getContentBody();
        UpgradeEvent upgradeEvent = readInscriptionContent(inscriptionContent, UpgradeEvent.class);
        if (upgradeEvent == null || !upgradeEvent.isValid()) {
            logger.info("invalid upgrade inscription:" + inscriptionId);
            return;
        }
        String tick = upgradeEvent.getTick();
        String id = upgradeEvent.getId();
        long blockHeight = inscription.getGenesisBlockHeight();
        Orc20Metadata orc20Metadata = getMetadata(tick, id, blockHeight);
        if (orc20Metadata == null) {
            logger.info("No tick deployed:" + inscriptionId);
            return;
        }
        String eventId = getEventId(inscriptionTransfer);
        Orc20Event orc20Event = new Orc20Event(eventId, EventType.TRANSFER_UPGRADE, OP.UPGRADE, inscriptionTransfer, inscription, orc20Metadata);
        String tickId = orc20Metadata.getTickId();
        List<Orc20Event> processedEvents = MemoryCache.selectOrc20EventByInscriptionIdAndEventType(inscriptionId, orc20Event.getEventType());
        if (processedEvents != null && processedEvents.size() > 0) {
            // has upgraded before
            orc20Event.setEventStatus(EventStatus.FAILED);
            orc20Event.setEventErrCode(EventErrCode.INVALID_UPGRADE_INSCRIPTION);
        } else {
            if (OIP.isUpgradeValidationAddress(receiver)) {
                String deployer = orc20Metadata.getDeployer();
                if (sender.equals(deployer)) {
                    boolean upgradeable = orc20Metadata.isUpgradeable();
                    if (upgradeable) {
                        if (upgradeEvent.getUg() != null) {
                            orc20Metadata.setUpgradeable(upgradeEvent.getUg());
                        }
                        if (upgradeEvent.getMax() != null) {
                            orc20Metadata.setMax(upgradeEvent.getMax());
                        }
                        if (upgradeEvent.getLim() != null) {
                            orc20Metadata.setLimit(upgradeEvent.getLim());
                        }
                        if (upgradeEvent.getDec() != null) {
                            orc20Metadata.setDecimals(upgradeEvent.getDec());
                        }
                        orc20Metadata.setUpgradeTime(inscriptionTransfer.getBlockTime());
                        orc20Metadata.setContent(inscriptionContent);
                        orc20Event.setEventStatus(EventStatus.SUCCESS);
                    } else {
                        orc20Event.setEventStatus(EventStatus.FAILED);
                        orc20Event.setEventErrCode(EventErrCode.NON_UPGRADEABLE);
                    }
                } else {
                    orc20Event.setEventStatus(EventStatus.FAILED);
                    orc20Event.setEventErrCode(EventErrCode.NO_UPGRADE_PERMISSION);
                }
            } else {
                orc20Event.setEventStatus(EventStatus.FAILED);
                orc20Event.setEventErrCode(EventErrCode.INVALID_UPGRADE_INSCRIPTION);
            }
        }
        EventStatus upgradeStatus = orc20Event.getEventStatus();
        EventErrCode upgradeErrCode = orc20Event.getEventErrCode();
        MemoryCache.updateOrc20EventStatusByTickIdAndInscriptionIdAndToAddress(tickId, inscriptionId, sender, EventStatus.UPGRADE_BLOCKED, upgradeStatus, upgradeErrCode);
        MemoryCache.updateOrc20EventStatusByTickIdAndInscriptionIdAndToAddress(tickId, inscriptionId, sender, EventStatus.UPGRADE_WAITING, upgradeStatus, upgradeErrCode);

        MemoryCache.insertOrc20Event(orc20Event);
    }

    /**
     * check if the event processed
     */
    private boolean eventProcessed(String eventId) {
        return MemoryCache.selectOrc20EventByEventId(eventId) != null;
    }

    /**
     * get metadata based on OIP3
     */
    private Orc20Metadata getMetadata(String tick, String id, long blockHeight) {
        Orc20Metadata metadata = null;
        if (OIP.beforeOIP3(blockHeight)) {
            metadata = MemoryCache.selectORC20MetadataByTickAndDeployId(tick, id);
        } else {
            // after oip3, "id" in json means the deployment inscription number
            try {
                // should be a valid number
                Long inscriptionNumber = NumberValidator.longFromString(id);
                if (inscriptionNumber != null) {
                    metadata = MemoryCache.selectORC20MetadataByTickAndInscriptionNumber(tick, inscriptionNumber);
                }
            } catch (NumberValidator.DecimalsValidatorException e) {
                // invalid inscription number
            }
        }
        return metadata;
    }

    /**
     * check json schema
     */
    private boolean isValidEvent(BaseEvent baseEvent, Orc20Event orc20Event) {
        if (baseEvent == null || !baseEvent.isValid()) {
            orc20Event.setEventStatus(EventStatus.FAILED);
            orc20Event.setEventErrCode(EventErrCode.INVALID_INSCRIPTION);
            MemoryCache.insertOrc20Event(orc20Event);
            return false;
        }
        return true;
    }

    /**
     * generate event id
     */
    private String getEventId(InscriptionTransfer inscriptionTransfer) {
        // location is unique on blockchain, we use it as eventId
        // Be careful of curse inscription!
        return inscriptionTransfer.getToLocation();
    }

    /**
     * https://docs.orc20.org/operations#deploy-or-migrate-event
     * "Decimal: decimal precision must be <=18, default to 18"
     */
    private boolean exceedingDecimal(BigDecimal bigDecimal, int dec, Orc20Event orc20Event) {
        if (bigDecimal != null && bigDecimal.scale() > dec) {
            if (orc20Event != null) { // "inscribe deploy" has no tick metadata
                orc20Event.setEventStatus(EventStatus.FAILED);
                orc20Event.setEventErrCode(EventErrCode.INVALID_INSCRIPTION);
                MemoryCache.insertOrc20Event(orc20Event);
            }
            return true;
        }
        return false;
    }

}
