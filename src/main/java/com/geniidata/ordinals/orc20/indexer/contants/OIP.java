package com.geniidata.ordinals.orc20.indexer.contants;

import java.math.BigDecimal;
import java.math.BigInteger;

public class OIP {
    public final static BigDecimal DEPLOY_MIN_DEFAULT = BigDecimal.ONE;
    public final static int DEPLOY_DEC_DEFAULT = 18;
    public final static int DEPLOY_DEC_MAX = 18;
    public final static boolean DEPLOY_WP_DEFAULT = false;
    public final static boolean DEPLOY_UPGRADABLE_DEFAULT = true;
    private final static String uInt256Str = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
    private final static BigInteger uInt256 = new BigInteger(uInt256Str, 16);
    public final static BigDecimal DEPLOY_MAX_UINT256 = new BigDecimal(uInt256);

    private final static long OIP_3_BLOCK = 788836;
    private final static String OIP_6_UPGRADE_VALIDATION_ADDRESS = "bc1pgha2vs4m4d70aw82qzrhmg98yea4fuxtnf7lpguez3z9cjtukpssrhakhl";
    private final static long OIP_10_BLOCK = 800010; // follow the final definition of OIP10
    private final static String OIP_10_VIRTUAL_ATM_ADDRESS = "bc1pgha2vs4m4d70aw82qzrhmg98yea4fuxtnf7lpguez3z9cjtukpssrhakhl";

    public static boolean beforeOIP3(long blockHeight) {
        return blockHeight < OIP_3_BLOCK;
    }

    public static boolean isUpgradeValidationAddress(String address) {
        return OIP_6_UPGRADE_VALIDATION_ADDRESS.equals(address);
    }

    public static boolean beforeOIP10(long blockHeight) {
        return blockHeight < OIP_10_BLOCK;
    }

    public static boolean isVirtualATMAddress(String address) {
        return OIP_10_VIRTUAL_ATM_ADDRESS.equals(address);
    }

}
