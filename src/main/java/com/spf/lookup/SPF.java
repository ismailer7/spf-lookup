package com.spf.lookup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SPF {

    static final int BATCH_SIZE = 10000;

    public static void main(String[] args) throws InterruptedException {

        var batch = 0;
        var domains = "";

        if(args.length == 2) {
            domains = args[0];
            batch = Integer.parseInt(args[1]);
        } else if(args.length == 1) {
          domains = args[0];
          batch = BATCH_SIZE;
        } else {
            System.out.println("Usage: java -jar spf-lookup.jar <file-path-of-domains> <number-of-threads>(default: 30)");
            return;
        }

        System.out.println("Batch Processing: " + batch);
        System.out.println("Loading Domains..");

        var domainList = new ArrayList<String>();

        try {
            domainList.addAll(FileUtils.loadFileIntoList(domains));
            System.out.println("Successfully Loaded " + domainList.size() + " Domain!");

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        System.out.println("Create Assets..");

        var folderResult = FileUtils.createResultFolder();
        var resultFile = FileUtils.createResultFile(folderResult);
        var checkedFile = FileUtils.createCheckedFile(folderResult);
        var expiredFile = FileUtils.createAvailableForSellFile(folderResult);
        var origin = FileUtils.createOriginalSpfFile(folderResult);

        System.out.println("Assets Directory and Files has been Created !!");
        var batches = partitions(domainList, batch);
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        batches.forEach(partition -> {
            executorService.execute(new SPFJobExecutor(partition, resultFile, checkedFile, origin));
        });
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.DAYS);

    }

    static Stream<List<String>> partitions(List<String> domains, int length) {
        int size = domains.size();
        if (size <= 0)
            return Stream.empty();
        int fullChunks = (size - 1) / length;
        return IntStream.range(0, fullChunks + 1).mapToObj(
                n -> domains.subList(n * length, n == fullChunks ? size : (n + 1) * length));
    }

}
