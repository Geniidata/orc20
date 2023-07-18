package com.geniidata.ordinals.orc20.indexer.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Json {
    private final static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // allow custom keys
        objectMapper.configure(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), true); // allow for a single trailing comma following the final value (in an Array) or member (in an Object)
        objectMapper.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true); // check all names within context and report duplicates by throwing a JsonParseException
        objectMapper.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, false); // no check is made for possible trailing token(s)
    }

    /**
     * encode
     */
    public static String writeValueAsString(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    /**
     * decode
     */
    public static <T> T readValue(String content, Class<T> cls) throws JsonProcessingException {
        return objectMapper.readValue(content, cls);
    }

    /**
     * decode
     */
    public static <T> T readValue(String content, TypeReference<T> typeReference) throws JsonProcessingException {
        return objectMapper.readValue(content, typeReference);
    }
}
