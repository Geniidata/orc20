package com.geniidata.ordinals.orc20.indexer;

import com.geniidata.ordinals.orc20.indexer.data.Dumper;
import com.geniidata.ordinals.orc20.indexer.data.Loader;
import com.geniidata.ordinals.orc20.indexer.model.InscriptionTransfer;
import com.geniidata.ordinals.orc20.indexer.storage.MemoryCache;
import org.apache.commons.cli.*;

import java.io.PrintWriter;

/**
 * Main Class
 */
public class Indexer {
    private final String contentInputPath;
    private final String transferInputPath;

    public Indexer(String contentInputPath, String transferInputPath) {
        this.contentInputPath = contentInputPath;
        this.transferInputPath = transferInputPath;
    }

    public static Indexer fromOptions(String[] args) {
        Options options = new Options();
        Option transferInput = new Option("t", "transfer", true, "Input file path for ORC20 inscription transfers");
        transferInput.setRequired(true);
        options.addOption(transferInput);
        Option contentInput = new Option("c", "content", true, "Input file path for ORC20 inscription contents");
        contentInput.setRequired(true);
        options.addOption(contentInput);
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            PrintWriter pw = new PrintWriter(System.err);
            formatter.printHelp(pw, formatter.getWidth(), "Indexer", null, options, formatter.getLeftPadding(), formatter.getDescPadding(), null);
            pw.flush();
            System.exit(1);
        }
        String contentInputPath = cmd.getOptionValue("content");
        String transferInputPath = cmd.getOptionValue("transfer");
        return new Indexer(contentInputPath, transferInputPath);
    }

    public static void main(String[] args) {
        Indexer indexer = Indexer.fromOptions(args);
        indexer.run();
    }

    public void run() {
        // load inscription contents & transfers from datasource
        Loader.loadInputs(contentInputPath, transferInputPath);

        // process
        Orc20 orc20 = new Orc20();
        for (InscriptionTransfer inscriptionTransfer : MemoryCache.selectInscriptionTransfers()) {
            orc20.accept(inscriptionTransfer);
        }

        // dump
        Dumper.summarizeOrc20Balance();
        Dumper.dumpOrc20Balance();
        Dumper.dumpOrc20Metadata();
        Dumper.dumpOrc20Event();
        Dumper.dumpOrc20BalanceOIP10Snapshot();
    }
}
