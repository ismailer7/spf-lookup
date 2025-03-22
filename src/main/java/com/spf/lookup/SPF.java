package com.spf.lookup;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SPF {

    static final int BATCH_SIZE = 10000;
    private static final AtomicInteger progressCounter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        printBanner();
        var batch = 0;
        var domains = "";

        if (args.length == 2) {
            domains = args[0];
            batch = Integer.parseInt(args[1]);
        } else if (args.length == 1) {
            domains = args[0];
            batch = BATCH_SIZE;
        } else {
            System.err.println("Usage: java -jar spf-lookup.jar <file-path-of-domains> <number-of-threads>(default: 30)");
            return;
        }

        System.out.println("Batch Processing: " + batch);
        //System.out.println("Loading Domains...");

        var domainList = new ArrayList<String>();

        try {
            domainList.addAll(FileUtils.loadFileIntoList(domains));
            System.out.println("Successfully Loaded " + domainList.size() + " Domains!");

        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }

        //System.out.println("Creating Assets...");
        animateProgressBar(5, "Setting up directories assets and files...");

        var folderResult = FileUtils.createResultFolder();
        var resultFile = FileUtils.createResultFile(folderResult);
        var checkedFile = FileUtils.createCheckedFile(folderResult);
        var expiredFile = FileUtils.createAvailableForSellFile(folderResult);
        var logFile = FileUtils.logFile(folderResult);
        var origin = FileUtils.createOriginalSpfFile(folderResult);

        System.out.println("\n✅ Assets Directory and Files Created!");
        var batches = partitions(domainList, batch);
        // Set total jobs for progress tracking
        SPFJobExecutor.setTotalJobs(domainList.size());
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        batches.forEach(partition -> {
            executorService.execute(new SPFJobExecutor(partition, resultFile, checkedFile, origin, logFile));
        });
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.DAYS);

        System.out.println("\n✅ Processing Complete!");
    }

    static Stream<List<String>> partitions(List<String> domains, int length) {
        int size = domains.size();
        if (size <= 0)
            return Stream.empty();
        int fullChunks = (size - 1) / length;
        return IntStream.range(0, fullChunks + 1).mapToObj(
                n -> domains.subList(n * length, n == fullChunks ? size : (n + 1) * length));
    }

    private static void printBanner() {
        String version = getAppVersion();

        // ANSI Color Codes
        String RED = "\u001B[31m";
        String GREEN = "\u001B[32m";
        String BLUE = "\u001B[34m";
        String CYAN = "\u001B[36m";
        String RESET = "\u001B[0m"; // Reset color

        try (InputStream is = SPF.class.getClassLoader().getResourceAsStream("banner.txt")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Replace ${app.version} with a colored version number
                        line = line.replace("${app.version}", GREEN + version + RESET);
                        System.out.println(CYAN + line + RESET);
                    }
                }
            } else {
                System.out.println(RED + "Banner not found!" + RESET);
            }
        } catch (Exception e) {
            System.err.println(RED + "Error loading banner: " + e.getMessage() + RESET);
        }
    }


    private static String getAppVersion() {
        try (InputStream is = SPF.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                Properties properties = new Properties();
                properties.load(is);
                return properties.getProperty("app.version", "Unknown");
            }
        } catch (Exception e) {
            System.err.println("Error loading version: " + e.getMessage());
        }
        return "Unknown";
    }

    private static void animateProgressBar(int steps, String message) {
        String BLUE = "\u001B[34m";
        String RESET = "\u001B[0m";
        int totalBars = 20;

        System.out.print(message + " ");
        for (int i = 0; i <= totalBars; i++) {
            int percentage = (i * 100) / totalBars;
            String progressBar = BLUE + "[" + "=".repeat(i) + " ".repeat(totalBars - i) + "]" + RESET;
            System.out.print("\r" + message + " " + progressBar + " " + percentage + "%");
            System.out.flush();
            try {
                Thread.sleep(steps * 100); // Simulating work being done
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println(); // Move to the next line after completion
    }
}
