package com.geniidata.ordinals.orc20.indexer.data;

import com.geniidata.ordinals.orc20.indexer.model.InscriptionContent;
import com.geniidata.ordinals.orc20.indexer.model.InscriptionTransfer;
import com.geniidata.ordinals.orc20.indexer.storage.MemoryCache;
import com.geniidata.ordinals.orc20.indexer.utils.Json;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * load inscription content and transfers from datasource
 */
public class Loader {

    public static void loadInputs(String contentsDataFilePath, String transfersDataFilePath) {
        loadContentFromJsonInputFile(contentsDataFilePath);
        loadTransferFromJsonInputFile(transfersDataFilePath);
    }

    /**
     * load all transfers data(json format)
     *
     * @param filePath file path
     */
    private static void loadTransferFromJsonInputFile(String filePath) {
        try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                InscriptionTransfer transfer = Json.readValue(line, InscriptionTransfer.class);
                MemoryCache.insertInscriptionTransfer(transfer);
                String inscriptionId = transfer.getInscriptionId();
                if (!transfer.isTransfer()) {
                    MemoryCache.selectInscriptionContentByInscriptionId(inscriptionId).setGenesisBlockHeight(transfer.getBlockHeight());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * load all contents data(json format)
     *
     * @param filePath file path
     */
    private static void loadContentFromJsonInputFile(String filePath) {
        try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                InscriptionContent inscription = Json.readValue(line, InscriptionContent.class);
                MemoryCache.insertInscriptionContent(inscription);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
