package com.spf.lookup;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.System.exit;

public class FileUtils {





    private static final SimpleDateFormat spfFormat = new SimpleDateFormat("YYYY-MM-dd_hhmmss");
    static List<String> loadFileIntoList(String path) throws IOException {
        File oldFormatFile = new File(path);
        InputStream inputStream = new FileInputStream(oldFormatFile);
        var oldFormats = new ArrayList<String>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                oldFormats.add(line);
            }
        } catch (IOException e) {
            throw new IOException("Cannot Read File " + path + " !");
        }
        return oldFormats;
    }

    static String createResultFolder() {
        var folderName = "spf_" + spfFormat.format(new Date());
        File dir = new File(folderName);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        return folderName;
    }

    static File createResultFile(String folder) {
        File file = new File("./" + folder + "/spf.txt");
        if(!file.exists()) {
            try {
                file.createNewFile();
                return file;
            } catch (IOException e) {
                System.out.println("Cannot Create Result File !");
            }
        }
        return null;
    }

    static File createCheckedFile(String folder) {
        File file = new File("./" + folder + "/checked.txt");
        if(!file.exists()) {
            try {
                file.createNewFile();
                return file;
            } catch (IOException e) {
                System.out.println("Cannot Create Checked File !");
            }
        }
        return null;
    }

    static File createAvailableForSellFile(String folder) {
        File file = new File("./" + folder + "/availableForSell.txt");
        if(!file.exists()) {
            try {
                file.createNewFile();
                return file;
            } catch (IOException e) {
                System.out.println("Cannot Create Expired File !");
            }
        }
        return null;
    }

    static File createOriginalSpfFile(String folder) {
        File file = new File("./" + folder + "/origin.txt");
        if(!file.exists()) {
            try {
                file.createNewFile();
                return file;
            } catch (IOException e) {
                System.out.println("Cannot Create Origin File !");
            }
        }
        return null;
    }

    static File logFile(String folder) {
        File file = new File("./" + folder + "/logs.txt");
        if(!file.exists()) {
            try {
                file.createNewFile();
                return file;
            } catch (IOException e) {
                System.out.println("Cannot Create Logs File !");
            }
        }
        return null;
    }

    static synchronized void write(File file, String line) throws IOException, InterruptedException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsolutePath(), true))) {
            writer.append(line).append("\n");
        } catch (IOException e) {
            System.err.println("Cannot Write to File !");
            exit(0);
        }
    }

}
