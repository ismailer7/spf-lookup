package com.spf.lookup;

import com.spf.config.ConfigLoader;
import com.spf.job.SPFJobExecutor;
import com.spf.licence.LicenceValidator;
import com.spf.utils.FileUtils;

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

    public static void main(String[] args) throws InterruptedException, IOException {

        String licenseKey = ConfigLoader.get("licence.key"); // Could also read from a local file or input
        // System.out.println("🚀 Validating Licence Key for User ...");
        if (!LicenceValidator.validateLicense(licenseKey)) {
            //System.out.println("Application terminated due to license failure.");
            System.exit(1);
        }

        // Proceed with app
        System.out.println("🚀 Application starting...");

        /*System.out.println(DomainUtils.getRootDomain("dazda.zdazd.azd.Azd.azdazd.test.com.au"));
        System.out.println(DomainUtils.getRootDomain("dazda.zdazd.azd.Azd.azdazd.test.au"));
        System.out.println(DomainUtils.getRootDomain("dazda.zdazd.azd.Azd.azdazd.test.com.br"));
        System.out.println(DomainUtils.getRootDomain("dazda.zdazd.azd.Azd.azdazd.test.co.uk"));
        System.out.println(DomainUtils.getRootDomain("dazda.zdazd.azd.Azd.azdazd.test.br"));
        System.out.println(DomainUtils.getRootDomain("dazda.zdazd.azd.Azd.azdazd.test.co.il"));
        System.out.println(DomainUtils.getRootDomain("dazda.zdazd.azd.Azd.azdazd.test.com.uk"));
        System.out.println(DomainUtils.getRootDomain("dazda.zdazd.azd.Azd.azdazd.test.co.br"));
        System.out.println(DomainUtils.getRootDomain("dazda.zdazd.azd.Azd.azdazd.test.com.au"));
        System.out.println(DomainUtils.getRootDomain("dazda.zdazd.azd.Azd.azdazd.test.com.au"));*/

        printBanner();
        var batch = 0;
        var domains = "";
        var ignoreDomainsPath = "";

        if(args.length == 3) {
            domains = args[0];
            batch = Integer.parseInt(args[1]);
            ignoreDomainsPath = args[2];
        } else if (args.length == 2) {
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
        IgnoredDomainList ignoredDomainList = new IgnoredDomainList();

        if(ignoreDomainsPath.isEmpty()) {
            ignoredDomainList.setDefaultIgnoredList();
        } else {
            ignoredDomainList.loadFromFile(ignoreDomainsPath);
        }
        var ignoreDomains = ignoredDomainList.getIgnoredList();
        var guideFile = FileUtils.createGuideFile(folderResult);
        var guideFile2 = FileUtils.createGuideFile2(folderResult);
        //var expiredFile = FileUtils.createAvailableForSellFile(folderResult);
        var logFile = FileUtils.logFile(folderResult);
        var origin = FileUtils.createOriginalSpfFile(folderResult);

        System.out.println("\n✅ Assets Directory and Files Created!");
        var batches = partitions(domainList, batch);
        // Set total jobs for progress tracking
        SPFJobExecutor.setTotalJobs(domainList.size());
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        batches.forEach(partition -> {
            executorService.execute(new SPFJobExecutor(partition, resultFile, checkedFile, origin, logFile, ignoredDomainList, guideFile, guideFile2));
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

    public static void printBanner() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        // ANSI color codes (disabled on Windows CMD but works in Windows Terminal/VS Code)
        String RESET = isWindows ? "" : "\033[0m";
        String CYAN = isWindows ? "" : "\033[0;36m";
        String YELLOW = isWindows ? "" : "\033[0;33m";

        String version = getAppVersion();

        try (InputStream is = SPF.class.getClassLoader().getResourceAsStream("banner.txt")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Apply color only if not on Windows CMD
                        System.out.println(CYAN + line.replace("${app.version}", version) + RESET);
                    }
                }
            } else {
                System.out.println("Banner file not found!");
            }
        } catch (Exception e) {
            System.err.println("Error loading banner: " + e.getMessage());
        }

        // Print version separately if banner.txt doesn't contain ${app.version}
        System.out.println(YELLOW + " Version: " + version + RESET);
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
                Thread.sleep(steps * 100L); // Simulating work being done
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println(); // Move to the next line after completion*/
    }
}
