package com.geniidata.ordinals.orc20.indexer.storage;

import com.geniidata.ordinals.orc20.indexer.enums.BalanceStatus;
import com.geniidata.ordinals.orc20.indexer.enums.EventErrCode;
import com.geniidata.ordinals.orc20.indexer.enums.EventStatus;
import com.geniidata.ordinals.orc20.indexer.enums.EventType;
import com.geniidata.ordinals.orc20.indexer.model.*;

import java.util.*;

/**
 * In-memory storage, can be replaced with other storage.
 */
public class MemoryCache {
    // table of all inscription contents
    private final static Map<String, InscriptionContent> inscriptionContentsTable = new HashMap<>();
    // table of all inscription transfers
    private final static ArrayList<InscriptionTransfer> inscriptionTransfersTable = new ArrayList<>();

    // table of all orc20 events
    private final static Map<String, Orc20Event> orc20EventsTable = new HashMap<>();
    // index(tickId, creator)
    private final static Map<String, Set<String>> orc20EventTickIdCreatorIndex = new HashMap<>();
    // index(tickId, address)
    private final static Map<String, Set<String>> orc20EventTickIdToAddressIndex = new HashMap<>();
    // index(inscriptionId, event)
    private final static Map<String, Set<String>> orc20EventInscriptionIdEventIndex = new HashMap<>();

    // table of all orc20 user balance
    private final static Map<String, Orc20Balance> orc20BalanceTable = new HashMap<>();
    private final static Map<String, Orc20Balance> orc20BalanceTableOIP10Snapshot = new HashMap<>();
    // index(tickId, address)
    private final static Map<String, Set<String>> orc20BalanceTickIdAddressIndex = new HashMap<>();
    // index(tickId, creator)
    private final static Map<String, Set<String>> orc20BalanceTickIdCreatorIndex = new HashMap<>();
    // index(inscriptionId) !!! "credit balance" has no inscriptionId !!!
    private final static Map<String, String> orc20BalanceInscriptionIdIndex = new HashMap<>();

    // table of all orc20 ticks
    private final static Map<String, Orc20Metadata> orc20MetadataTable = new HashMap<>();
    // index(tick, inscriptionNumber)
    private final static Map<String, String> orc20MetadataTickInscriptionNumberIndex = new HashMap<>();
    // index(tick, deployId)
    private final static Map<String, String> orc20MetadataTickDeployIdIndex = new HashMap<>();

    /**
     * concatenate index fields and use base64 to avoid conflicts
     */
    private static String indexKey(Object... keys) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Object key : keys) {
            if (key == null) {
                stringBuilder.append("_NULL_");
            } else {
                stringBuilder.append(Base64.getEncoder().encodeToString(key.toString().getBytes()));
                stringBuilder.append("-");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * save inscription content
     */
    public static void insertInscriptionContent(InscriptionContent inscription) {
        inscriptionContentsTable.put(inscription.getInscriptionId(), inscription);
    }

    /**
     * save inscription transfer
     */
    public static void insertInscriptionTransfer(InscriptionTransfer transfer) {
        inscriptionTransfersTable.add(transfer);
    }

    /**
     * get all inscription transfers, ordered by block_height and transaction_index
     */
    public static ArrayList<InscriptionTransfer> selectInscriptionTransfers() {
        inscriptionTransfersTable.sort(Comparator.comparingLong(InscriptionTransfer::getBlockHeight).thenComparingLong(InscriptionTransfer::getTxIndex));
        return inscriptionTransfersTable;
    }

    /**
     * get inscription content for the given inscriptionId
     */
    public static InscriptionContent selectInscriptionContentByInscriptionId(String inscriptionId) {
        return inscriptionContentsTable.get(inscriptionId);
    }

    /**
     * save/update orc20event and refresh relevant indexes
     */
    public static void insertOrc20Event(Orc20Event orc20Event) {
        String primaryKey = orc20Event.getEventId();
        orc20EventsTable.put(primaryKey, orc20Event);

        // index for accelerating queries
        String tickId = orc20Event.getTickId();
        String creator = orc20Event.getCreator();
        String indexKey = indexKey(tickId, creator);
        if (!orc20EventTickIdCreatorIndex.containsKey(indexKey)) {
            orc20EventTickIdCreatorIndex.put(indexKey, new HashSet<>());
        }
        orc20EventTickIdCreatorIndex.get(indexKey).add(primaryKey);

        // index for accelerating queries
        String address = orc20Event.getToAddress();
        indexKey = indexKey(tickId, address);
        if (!orc20EventTickIdToAddressIndex.containsKey(indexKey)) {
            orc20EventTickIdToAddressIndex.put(indexKey, new HashSet<>());
        }
        orc20EventTickIdToAddressIndex.get(indexKey).add(primaryKey);

        // index for accelerating queries
        String inscriptionId = orc20Event.getInscriptionId();
        indexKey = indexKey(inscriptionId, orc20Event.getEventType().name());
        if (!orc20EventInscriptionIdEventIndex.containsKey(indexKey)) {
            orc20EventInscriptionIdEventIndex.put(indexKey, new HashSet<>());
        }
        orc20EventInscriptionIdEventIndex.get(indexKey).add(primaryKey);

    }

    /**
     * get event for the given eventId
     */
    public static Orc20Event selectOrc20EventByEventId(String eventId) {
        return orc20EventsTable.get(eventId);
    }

    /**
     * get events by (inscriptionId, eventType)
     */
    public static List<Orc20Event> selectOrc20EventByInscriptionIdAndEventType(String inscriptionId, EventType eventType) {
        String indexKey = indexKey(inscriptionId, eventType.name());
        List<Orc20Event> orc20EventList = new ArrayList<>();
        if (orc20EventInscriptionIdEventIndex.containsKey(indexKey)) {
            orc20EventInscriptionIdEventIndex.get(indexKey).forEach(
                    key -> orc20EventList.add(orc20EventsTable.get(key))
            );
        }
        return orc20EventList;
    }

    /**
     * get all pending events created by `creator`
     * generally used to update the pending "inscribe send" event status upon receiving the "remaining balance" or "cancel" inscription.
     */
    public static List<Orc20Event> selectPendingOrc20EventListByTickIdAndCreator(String tickId, String creator) {
        List<Orc20Event> eventList = new ArrayList<>();
        String indexKey = indexKey(tickId, creator);
        if (orc20EventTickIdCreatorIndex.containsKey(indexKey)) {
            orc20EventTickIdCreatorIndex.get(indexKey).forEach(
                    key -> {
                        Orc20Event event = orc20EventsTable.get(key);
                        if (event.getEventStatus().equals(EventStatus.SEND_PENDING)) {
                            eventList.add(event);
                        }
                    }
            );
        }
        return eventList;
    }

    /**
     * generally used to update the state of non-terminal events, e.g., UPGRADE_BLOCKED, UPGRADE_WAITING
     */
    public static List<Orc20Event> selectOrc20EventListByTickIdAndToAddress(String tickId, String address) {
        List<Orc20Event> eventList = new ArrayList<>();
        String indexKey = indexKey(tickId, address);
        if (orc20EventTickIdToAddressIndex.containsKey(indexKey)) {
            orc20EventTickIdToAddressIndex.get(indexKey).forEach(
                    key -> eventList.add(orc20EventsTable.get(key))
            );
        }
        return eventList;
    }

    /**
     * generally used to update the pending "inscribe send" events status upon receiving the "remaining balance" or "cancel" inscription.
     */
    public static void updateOrc20EventStatusByTickIdAndCreator(String tickId, String creator, EventStatus oldStatus, EventStatus newStatus) {
        List<Orc20Event> eventList = selectPendingOrc20EventListByTickIdAndCreator(tickId, creator);
        eventList.forEach(
                e -> {
                    if (e.getEventStatus().equals(oldStatus)) {
                        e.setEventStatus(newStatus);
                    }
                }
        );
    }

    /**
     * used to cancel the "inscribe send" event with the specific nonce
     */
    public static void updateOrc20EventStatusByTickIdAndCreatorAndNonce(String tickId, String creator, long nonce, EventStatus newStatus) {
        List<Orc20Event> eventList = selectPendingOrc20EventListByTickIdAndCreator(tickId, creator);
        eventList.forEach(
                e -> {
                    if (e.getNonce() == nonce) {
                        e.setEventStatus(newStatus);
                    }
                }
        );
    }

    /**
     * generally used to update the state of non-terminal events, e.g., UPGRADE_BLOCKED, UPGRADE_WAITING
     */
    public static void updateOrc20EventStatusByTickIdAndToAddress(String tickId, String address, EventStatus oldStatus, EventStatus newStatus, EventErrCode newErrCode) {
        List<Orc20Event> eventList = selectOrc20EventListByTickIdAndToAddress(tickId, address);
        eventList.forEach(
                e -> {
                    if (e.getEventStatus().equals(oldStatus)) {
                        e.setEventStatus(newStatus);
                        e.setEventErrCode(newErrCode);
                    }
                }
        );
    }

    /**
     * generally used to update the state of non-terminal events, e.g., UPGRADE_BLOCKED, UPGRADE_WAITING
     */
    public static void updateOrc20EventStatusByTickIdAndInscriptionIdAndToAddress(String tickId, String inscriptionId, String address, EventStatus oldStatus, EventStatus newStatus, EventErrCode newErrCode) {
        List<Orc20Event> eventList = selectOrc20EventListByTickIdAndToAddress(tickId, address);
        eventList.forEach(
                e -> {
                    if (e.getEventStatus().equals(oldStatus) && e.getInscriptionId().equals(inscriptionId)) {
                        e.setEventStatus(newStatus);
                        e.setEventErrCode(newErrCode);
                    }
                }
        );
    }

    /**
     * store/update tick metadata and refresh relevant indexes
     */
    public static void insertOrc20Metadata(Orc20Metadata orc20Metadata) {
        String tick = orc20Metadata.getTick();
        long inscriptionNumber = orc20Metadata.getInscriptionNumber();
        String deployId = orc20Metadata.getDeployId();

        String primaryKey = orc20Metadata.getTickId();
        orc20MetadataTable.put(primaryKey, orc20Metadata);

        // index for accelerating queries
        String indexKey = indexKey(tick, inscriptionNumber);
        orc20MetadataTickInscriptionNumberIndex.put(indexKey, primaryKey);

        // index for accelerating queries
        indexKey = indexKey(tick, deployId);
        orc20MetadataTickDeployIdIndex.put(indexKey, primaryKey);
    }

    /**
     * get tick metadata by (tick, deploymentInscriptionNumber)
     */
    public static Orc20Metadata selectORC20MetadataByTickAndInscriptionNumber(String tick, long inscriptionNumber) {
        String indexKey = indexKey(tick, inscriptionNumber);
        String tickId = orc20MetadataTickInscriptionNumberIndex.get(indexKey);
        if (tickId != null) {
            return orc20MetadataTable.get(tickId);
        }
        return null;
    }

    /**
     * get tick metadata by tickId(primary key)
     */
    public static Orc20Metadata selectORC20MetadataByTickId(String tickId) {
        return orc20MetadataTable.get(tickId);
    }

    /**
     * get tick metadata by (tick, deployId)
     * deployId ( "id" in json before oip3, deployment inscription number after oip3)
     */
    public static Orc20Metadata selectORC20MetadataByTickAndDeployId(String tick, String deployId) {
        String indexKey = indexKey(tick, deployId);
        String tickId = orc20MetadataTickDeployIdIndex.get(indexKey);
        if (tickId != null) {
            return orc20MetadataTable.get(tickId);
        }
        return null;
    }

    /**
     * store/update user balance and refresh relevant indexes
     */
    public static void insertOrc20Balance(Orc20Balance orc20Balance) {
        String primaryKey = String.format("%s-%s-%s", orc20Balance.getTickId(), orc20Balance.getInscriptionId(), orc20Balance.getAddress());
        orc20BalanceTable.put(primaryKey, orc20Balance);

        // index for accelerating queries
        String tickId = orc20Balance.getTickId();
        String address = orc20Balance.getAddress();
        String indexKey = indexKey(tickId, address);
        if (!orc20BalanceTickIdAddressIndex.containsKey(indexKey)) {
            orc20BalanceTickIdAddressIndex.put(indexKey, new HashSet<>());
        }
        orc20BalanceTickIdAddressIndex.get(indexKey).add(primaryKey);

        // index for accelerating queries
        String creator = orc20Balance.getCreator();
        indexKey = indexKey(tickId, creator);
        if (!orc20BalanceTickIdCreatorIndex.containsKey(indexKey)) {
            orc20BalanceTickIdCreatorIndex.put(indexKey, new HashSet<>());
        }
        orc20BalanceTickIdCreatorIndex.get(indexKey).add(primaryKey);

        // index for accelerating queries
        String inscriptionId = orc20Balance.getInscriptionId();
        if (inscriptionId != null && !"".equals(inscriptionId)) {
            // Note: The "credit balance" does not have an inscriptionId
            orc20BalanceInscriptionIdIndex.put(inscriptionId, primaryKey);
        }

    }

    /**
     * get user balance list by (tickId, address)
     */
    public static List<Orc20Balance> selectOrc20BalanceListByTickIdAndAddress(String tickId, String address) {
        List<Orc20Balance> balanceList = new ArrayList<>();
        String indexKey = indexKey(tickId, address);
        if (orc20BalanceTickIdAddressIndex.containsKey(indexKey)) {
            Set<String> balanceInscriptionList = orc20BalanceTickIdAddressIndex.get(indexKey);
            if (balanceInscriptionList != null) {
                balanceInscriptionList.forEach(i -> balanceList.add(orc20BalanceTable.get(i)));
            }
        }
        return balanceList;
    }

    /**
     * get pending user balance list by (tickId, creator)
     */
    public static List<Orc20Balance> selectPendingOrc20BalanceListByTickIdAndCreator(String tickId, String creator) {
        List<Orc20Balance> balanceList = new ArrayList<>();
        String indexKey = indexKey(tickId, creator);
        if (orc20BalanceTickIdCreatorIndex.containsKey(indexKey)) {
            Set<String> balanceInscriptionList = orc20BalanceTickIdCreatorIndex.get(indexKey);
            if (balanceInscriptionList != null) {
                balanceInscriptionList.forEach(i -> {
                    Orc20Balance balance = orc20BalanceTable.get(i);
                    if (balance.getBalanceStatus().equals(BalanceStatus.SEND_PENDING)) {
                        balanceList.add(balance);
                    }
                });
            }
        }
        return balanceList;
    }

    /**
     * get user balance by (inscriptionId)
     * Note: The "credit balance" does not have an inscriptionId and cannot be retrieved using this method.
     */
    public static Orc20Balance selectOrc20BalanceByInscriptionId(String inscriptionId) {

        String primaryKey = orc20BalanceInscriptionIdIndex.get(inscriptionId);
        if (primaryKey != null) {
            return orc20BalanceTable.get(primaryKey);
        }
        return null;
    }

    /**
     * update balance status by (tickId, creator, nonce)
     * used to mark the balance maintained on the "inscribe send" inscription that has been canceled as invalid
     */
    public static void updateOrc20BalanceStatusByTickIdAndCreatorAndNonce(String tickId, String creator, long nonce, BalanceStatus newBalanceStatus) {
        List<Orc20Balance> balanceList = selectPendingOrc20BalanceListByTickIdAndCreator(tickId, creator);
        balanceList.forEach(
                b -> {
                    if (b.getNonce() == nonce) {
                        b.setBalanceStatus(newBalanceStatus);
                    }
                }
        );
    }

    /**
     * change balance holder
     * generally used to update the holder of balance after "transfer mint" or "transfer send"
     */
    public static void updateOrc20BalanceAddressByTickIdAndInscriptionId(String tickId, String inscriptionId, String oldAddress, String newAddress) {
        String primaryKey = String.format("%s-%s-%s", tickId, inscriptionId, oldAddress);
        String indexKey = indexKey(tickId, oldAddress);
        orc20BalanceTickIdAddressIndex.get(indexKey).remove(primaryKey);
        Orc20Balance orc20Balance = orc20BalanceTable.remove(primaryKey);
        String creator = orc20Balance.getCreator();
        indexKey = indexKey(tickId, creator);
        orc20BalanceTickIdCreatorIndex.get(indexKey).remove(primaryKey);
        orc20Balance.setAddress(newAddress);
        insertOrc20Balance(orc20Balance);
    }

    /**
     * create balance snapshot
     */
    public static void createBalanceSnapshot() {
        orc20BalanceTableOIP10Snapshot.clear();
        dumpOrc20Balance().forEach(
                (key, value) -> orc20BalanceTableOIP10Snapshot.put(key, new Orc20Balance(value))
        );
    }

    public static Map<String, Orc20Event> dumpOrc20Event() {
        return orc20EventsTable;
    }

    public static Map<String, Orc20Metadata> dumpOrc20Metadata() {
        return orc20MetadataTable;
    }

    public static Map<String, Orc20Balance> dumpOrc20Balance() {
        return orc20BalanceTable;
    }

    public static Map<String, Orc20Balance> dumpOrc20BalanceOIP10Snapshot() {
        return orc20BalanceTableOIP10Snapshot;
    }

}
