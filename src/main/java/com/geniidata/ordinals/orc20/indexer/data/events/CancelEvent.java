package com.geniidata.ordinals.orc20.indexer.data.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.geniidata.ordinals.orc20.indexer.utils.Json;

import java.util.ArrayList;
import java.util.List;

import static com.geniidata.ordinals.orc20.indexer.data.NumberValidator.longFromString;

/**
 * https://docs.orc20.org/operations#cancel-event
 */
public class CancelEvent extends BaseEvent {
    private List<Long> n; // "[1,2,3]"

    public List<Long> getN() {
        return n;
    }

    public void setN(String n) {
        // transfer the array string to number array
        try {
            List<String> noncesList = Json.readValue(n, new TypeReference<List<String>>() {
            });
            this.n = new ArrayList<>();
            for (String nonce : noncesList) {
                // validate number
                Long longN = longFromString(nonce);
                if (this.n.contains(longN)) {
                    continue;
                }
                this.n.add(longN);
            }
        } catch (JsonProcessingException e) {
            this.n = null;
        }
    }

    @Override
    public boolean isValid() {

        return super.isValid()
                && getId() != null // required
                && getN() != null; // required

    }

}
