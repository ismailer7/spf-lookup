package com.spf.lookup;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SPFJobExecutor implements Runnable {

    private final List<String> batch;
    private File resultFile;
    private File checkedFile;
    private File origin;

    private File logFile;

    public SPFJobExecutor(List<String> batch, File resultFile, File checkedFile, File origin, File logFile) {
        this.batch = batch;
        this.resultFile = resultFile;
        this.checkedFile = checkedFile;
        this.origin = origin;
        this.logFile = logFile;
    }

    @Override
    public void run() {
        for (String domain: batch) {
            try {
                SpfChecker spfChecker = new SpfChecker(domain);
                spfChecker.check(domain, 0, resultFile, checkedFile, origin, logFile);
                FileUtils.write(checkedFile, domain);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
