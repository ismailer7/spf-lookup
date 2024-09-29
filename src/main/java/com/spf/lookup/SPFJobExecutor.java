package com.spf.lookup;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SPFJobExecutor implements Runnable {


    private final List<String> batch;
    private File resultFile;

    private File checkedFile;

    public SPFJobExecutor(List<String> batch, File resultFile, File checkedFile) {
        this.batch = batch;
        this.resultFile = resultFile;
        this.checkedFile = checkedFile;
    }

    @Override
    public void run() {

        for (String domain: batch) {
            // process domain.
            //System.out.println("processing: " + domain);
            SpfChecker spfChecker = new SpfChecker(domain);
            spfChecker.check(domain, 0, resultFile, checkedFile);
            try {
                FileUtils.write(checkedFile, domain);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
