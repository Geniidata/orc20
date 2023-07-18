package com.geniidata.ordinals.orc20.indexer.enums;

public enum EventErrCode {
    REDEPLOYMENT,
    EXCEEDING_SUPPLY,
    EXCEEDING_LIMIT,
    INVALID_INSCRIPTION,
    INEFFECTIVE_INSCRIPTION,
    DUPLICATED_NONCE,
    NO_UPGRADE_PERMISSION,
    INSUFFICIENT_BALANCE,
    MISSING_INSCRIBE_SEND,
    NON_UPGRADEABLE,
    INVALID_UPGRADE_INSCRIPTION,
    REMAINING_BALANCE_LOCKED // After OIP-10, send the "Send Remaining Balance" to virtual ATM for credit.remaining balance will be permanently in a locked state after sending to Non-ATM
}
