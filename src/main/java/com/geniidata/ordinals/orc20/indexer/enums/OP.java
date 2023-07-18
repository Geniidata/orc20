package com.geniidata.ordinals.orc20.indexer.enums;

public enum OP {
    DEPLOY, MINT, SEND, REMAINING_BALANCE, SHADOW_REMAINING_BALANCE // OIP10: used to lock the remaining balance(after transferred to Non-ATM)
    , CANCEL, UPGRADE, _VIRTUAL_CREDIT_ // OIP10: used to identify credit balance
}
