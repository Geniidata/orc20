package com.geniidata.ordinals.orc20.indexer.data;

import com.geniidata.ordinals.orc20.indexer.utils.StringUtils;

import java.math.BigDecimal;

/**
 * validate the legality of string-type numbers in JSON
 */
public class NumberValidator {

    /**
     * 1.Not blank string
     * 2.Allow leading and trailing whitespace
     * 3.Not allowed to start with a plus sign
     * 4.Can parse by Java Integer
     *
     * @param numberString number
     * @return an integer if valid, otherwise throw an exception
     */
    public static Integer intFromString(String numberString) {
        if (numberString == null) {
            return null;
        }
        if (StringUtils.isBlank(numberString)) {
            throw new DecimalsValidatorException();
        }
        numberString = numberString.trim();
        if (numberString.startsWith("+")) {
            throw new DecimalsValidatorException();
        }
        try {
            return Integer.parseInt(numberString);
        } catch (Exception e) {
            throw new DecimalsValidatorException();
        }
    }

    /**
     * 1.Not blank string
     * 2.Allow leading and trailing whitespace
     * 3.Not allowed to start with a plus sign
     * 4.Can parse by Java Long
     *
     * @param numberString number
     * @return a long if valid, otherwise throw an exception
     */
    public static Long longFromString(String numberString) {
        if (numberString == null) {
            return null;
        }
        if (StringUtils.isBlank(numberString)) {
            throw new DecimalsValidatorException();
        }
        numberString = numberString.trim();
        if (numberString.startsWith("+")) {
            throw new DecimalsValidatorException();
        }
        try {
            return Long.parseLong(numberString);
        } catch (Exception e) {
            throw new DecimalsValidatorException();
        }
    }

    /**
     * 1.Not blank string
     * 2.Allow leading and trailing whitespace
     * 3.Not allowed to start with a plus sign
     * 4.Leading or trailing decimal points are not allowed
     * 5.At most one decimal point is allowed
     * 6.Can parse by Java BigDecimal
     *
     * @param numberString number
     * @return a bigdecimal if valid, otherwise throw an exception
     */
    public static BigDecimal decimalFromString(String numberString) {
        if (numberString == null) {
            return null;
        }
        if (StringUtils.isBlank(numberString)) {
            throw new DecimalsValidatorException();
        }
        numberString = numberString.trim();
        if (numberString.startsWith("+")) {
            throw new DecimalsValidatorException();
        }
        if (numberString.startsWith(".")) {
            throw new DecimalsValidatorException();
        }
        if (numberString.endsWith(".")) {
            throw new DecimalsValidatorException();
        }
        String[] parts = numberString.split("\\.");
        if (parts.length > 2) {
            throw new DecimalsValidatorException();
        }
        try {
            return new BigDecimal(numberString);
        } catch (Exception e) {
            throw new DecimalsValidatorException();
        }
    }

    public static class DecimalsValidatorException extends RuntimeException {
    }
}
