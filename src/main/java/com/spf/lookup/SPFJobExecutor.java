package com.spf.lookup;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SPFJobExecutor implements Runnable {

    private final List<String> batch;
    private File resultFile;
    private File checkedFile;
    private File origin;

    public SPFJobExecutor(List<String> batch, File resultFile, File checkedFile, File origin) {
        this.batch = batch;
        this.resultFile = resultFile;
        this.checkedFile = checkedFile;
        this.origin = origin;
    }

    @Override
    public void run() {
        for (String domain: batch) {
            SpfChecker spfChecker = new SpfChecker(domain);
            spfChecker.check(domain, 0, resultFile, checkedFile, origin);
            try {
                FileUtils.write(checkedFile, domain);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
