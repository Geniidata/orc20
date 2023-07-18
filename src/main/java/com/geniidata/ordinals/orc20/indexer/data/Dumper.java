package com.geniidata.ordinals.orc20.indexer.data;

import com.geniidata.ordinals.orc20.indexer.enums.BalanceStatus;
import com.geniidata.ordinals.orc20.indexer.enums.OP;
import com.geniidata.ordinals.orc20.indexer.model.Orc20Balance;
import com.geniidata.ordinals.orc20.indexer.model.Orc20Event;
import com.geniidata.ordinals.orc20.indexer.model.Orc20Metadata;
import com.geniidata.ordinals.orc20.indexer.storage.MemoryCache;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * dump the indexer results for test
 */
public class Dumper {
    public static void dumpOrc20Balance() {
        System.out.println("################ balance dump ################");
        MemoryCache.dumpOrc20Balance().values().stream()
                .filter(orc20Balance -> orc20Balance.getBalanceStatus().equals(BalanceStatus.OK))
                .sorted(Comparator.comparing(Orc20Balance::getAddress).thenComparing(Orc20Balance::getTickId))
                .forEach(System.out::println);
    }

    public static void dumpOrc20BalanceOIP10Snapshot() {
        System.out.println("################ balance oip10 snapshot dump ################");
        MemoryCache.dumpOrc20BalanceOIP10Snapshot().values().stream()
                .filter(orc20Balance -> orc20Balance.getBalanceStatus().equals(BalanceStatus.OK))
                .sorted(Comparator.comparing(Orc20Balance::getAddress).thenComparing(Orc20Balance::getTickId))
                .forEach(System.out::println);
    }

    /**
     * summarize balance by (address, tick_id)
     */
    public static void summarizeOrc20Balance() {
        // address, tick, inscription_number, BigDecimal
        TreeMap<BalanceSummaryKey, BalanceSummaryValue> userBalance = new TreeMap<>();
        MemoryCache.dumpOrc20Balance().values().stream()
                .filter(orc20Balance -> orc20Balance.getBalanceStatus().equals(BalanceStatus.OK))
                .sorted(Comparator.comparing(Orc20Balance::getAddress).thenComparing(Orc20Balance::getTickId))
                .forEach(
                        b -> {
                            String address = b.getAddress();
                            String tick = b.getTick();
                            String tickId = b.getTickId();
                            long tickNumber = MemoryCache.dumpOrc20Metadata().get(tickId).getInscriptionNumber();
                            BalanceSummaryValue summaryValue;
                            if (OP._VIRTUAL_CREDIT_.equals(b.getOp())) {
                                summaryValue = new BalanceSummaryValue(BigDecimal.ZERO, b.getBalance());
                            } else {
                                summaryValue = new BalanceSummaryValue(b.getBalance(), BigDecimal.ZERO);
                            }
                            BalanceSummaryKey summaryKey = new BalanceSummaryKey(address, tick, tickNumber);
                            if (userBalance.containsKey(summaryKey)) {
                                summaryValue.add(userBalance.get(summaryKey));
                            }
                            userBalance.put(summaryKey, summaryValue);
                        });

        System.out.println("################ balance summary ################");
        System.out.printf("%-64s\t%-18s\t%-18s\t%-18s\t%-18s\n", "Address", "Tick", "Inscription Number", "Cash Balance", "Credit Balance");
        for (Map.Entry<BalanceSummaryKey, BalanceSummaryValue> entry : userBalance.entrySet()) {
            BalanceSummaryKey key = entry.getKey();
            BalanceSummaryValue value = entry.getValue();
            String address = key.getAddress();
            System.out.printf("%-64s\t%-18s\t%-18d\t%-18s\t%-18s\n", address, key.getTick(), key.getTickNumber(), value.getCashBalance(), value.getCreditBalance());
        }

    }

    public static void dumpOrc20Metadata() {
        System.out.println("################ metadata dump ################");
        MemoryCache.dumpOrc20Metadata().values().stream()
                .sorted(Comparator.comparingLong(Orc20Metadata::getDeployTime))
                .forEach(System.out::println);
    }

    public static void dumpOrc20Event() {
        System.out.println("################ event dump ################");
        MemoryCache.dumpOrc20Event().values().stream()
                .sorted(Comparator.comparing(Orc20Event::getTickId).thenComparingLong(Orc20Event::getBlockHeight).thenComparingInt(Orc20Event::getTxIndex))
                .forEach(System.out::println);
    }

    private static class BalanceSummaryKey implements Comparable<BalanceSummaryKey> {
        private final String address;
        private final String tick;
        private final long tickNumber;

        public BalanceSummaryKey(String address, String tick, long tickNumber) {
            this.address = address;
            this.tick = tick;
            this.tickNumber = tickNumber;
        }

        public String getAddress() {
            return address;
        }

        public String getTick() {
            return tick;
        }

        public long getTickNumber() {
            return tickNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BalanceSummaryKey that = (BalanceSummaryKey) o;
            return tickNumber == that.tickNumber && Objects.equals(address, that.address) && Objects.equals(tick, that.tick);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address, tick, tickNumber);
        }

        @Override
        public int compareTo(BalanceSummaryKey o) {
            return Comparator.comparing(BalanceSummaryKey::getAddress)
                    .thenComparing(BalanceSummaryKey::getTick)
                    .thenComparingLong(BalanceSummaryKey::getTickNumber)
                    .compare(this, o);
        }
    }

    private static class BalanceSummaryValue {
        private BigDecimal cashBalance;
        private BigDecimal creditBalance;

        public BalanceSummaryValue(BigDecimal cashBalance, BigDecimal creditBalance) {
            this.cashBalance = cashBalance;
            this.creditBalance = creditBalance;
        }

        public void add(BalanceSummaryValue summaryValue) {
            this.cashBalance = this.cashBalance.add(summaryValue.cashBalance);
            this.creditBalance = this.creditBalance.add(summaryValue.creditBalance);
        }

        public BigDecimal getCashBalance() {
            return cashBalance;
        }

        public BigDecimal getCreditBalance() {
            return creditBalance;
        }
    }
}
