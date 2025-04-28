package com.spf.job;

import com.spf.lookup.IgnoredDomainList;
import com.spf.lookup.SpfChecker;
import com.spf.utils.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SPFJobExecutor implements Runnable {
    private final List<String> domains;
    private final File resultFile;
    private final File checkedFile;
    private final File originalFile;
    private final File logFile;
    private final IgnoredDomainList ignoredDomainList;
    private final File guideFile;
    private final File guideFile2;

    private static final AtomicInteger completedJobs = new AtomicInteger(0);
    private static int totalJobs = 1; // Default to avoid division by zero

    public SPFJobExecutor(List<String> domains, File resultFile, File checkedFile, File originalFile, File logFile, IgnoredDomainList ignoredDomainList, File guideFile, File guideFile2) {
        this.domains = domains;
        this.resultFile = resultFile;
        this.checkedFile = checkedFile;
        this.originalFile = originalFile;
        this.logFile = logFile;
        this.ignoredDomainList = ignoredDomainList;
        this.guideFile = guideFile;
        this.guideFile2 = guideFile2;
    }

    public static void setTotalJobs(int jobs) {
        totalJobs = jobs;
    }

    @Override
    public void run() {
        for (String domain : domains) {
            processDomain(domain);

            try {
                SpfChecker spfChecker = new SpfChecker();
                spfChecker.check(domain, resultFile, checkedFile, originalFile, logFile, ignoredDomainList, guideFile, guideFile2);
                FileUtils.write(checkedFile, domain);
            } catch (IOException | InterruptedException e) {
                System.err.println("[ERROR] Failed to process domain: " + domain);
            }

            int progress = (completedJobs.incrementAndGet() * 100) / totalJobs;
            updateProgressBar(progress);
        }
    }

    private void processDomain(String domain) {
        try (FileWriter writer = new FileWriter(resultFile, true)) {
            String spfResult = "SPF record for " + domain + ": PASS\n";
            writer.write(spfResult);
        } catch (IOException e) {
            System.err.println("[ERROR] Error processing domain " + domain + ": " + e.getMessage());
        }
    }

    private static void updateProgressBar(int percentage) {
        String GREEN = "\u001B[32m";
        String RESET = "\u001B[0m";

        int totalBars = 50;
        int filledBars = (percentage * totalBars) / 100;
        String progressBar = GREEN + "[" + "=".repeat(filledBars) + " ".repeat(totalBars - filledBars) + "]" + RESET;

        // Print on the same line and flush
        System.out.print("\rProgress: " + progressBar + " " + percentage + "%");
        System.out.flush();
    }
}
