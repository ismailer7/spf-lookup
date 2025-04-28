package com.spf.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.exit;

public class FileUtils {

    private static final SimpleDateFormat spfFormat = new SimpleDateFormat("YYYY-MM-dd_hhmmss");
    public static List<String> loadFileIntoList(String path) throws IOException {
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

    public static String createResultFolder() {
        var folderName = "spf_" + spfFormat.format(new Date());
        File dir = new File(folderName);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        return folderName;
    }

    public static File createResultFile(String folder) {
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

    static File createFile(String folder, String filename) {
        File file = new File("./" + folder + "/" + filename + ".txt");
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

    public static File createCheckedFile(String folder) {
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

    public static File createGuideFile(String folder) {
        File file = new File("./" + folder + "/guide.txt");
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

    public static File createGuideFile2(String folder) {
        File file = new File("./" + folder + "/guide2.txt");
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

    public static File createOriginalSpfFile(String folder) {
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

    public static File logFile(String folder) {
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

    public static synchronized void write(File file, String line) throws IOException, InterruptedException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsolutePath(), true))) {
            writer.append(line).append("\n");
        } catch (IOException e) {
            System.err.println("Cannot Write to File !");
            exit(0);
        }
    }

    static synchronized List<String> readFile(File file) throws IOException, InterruptedException {
        var ignoredDomainList = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
            Stream<String> result = reader.lines().map(String::toLowerCase).map(String::strip);
            return result.collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Cannot Write to File !");
            exit(0);
        }
        return new ArrayList<>();
    }

    public static void loadIgnoredDomains(String filePath, List<String> prefixes, List<String> suffixes, List<String> contains, List<String> exact, List<Pattern> regexes) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(filePath));

        for (String line : lines) {
            line = line.trim().toLowerCase();

            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.startsWith("^") || line.endsWith("$")) {
                regexes.add(Pattern.compile(line));
            } else if (line.startsWith(".")) {
                suffixes.add(line.substring(1)); // ".google.com" → "google.com"
            } else if (line.endsWith(".")) {
                prefixes.add(line.substring(0, line.length() - 1)); // "_spf." → "_spf"
            } else if (line.contains("*") || line.contains(".*")) {
                regexes.add(Pattern.compile(line.replace("*", ".*")));
            } else if (line.contains(".")) {
                exact.add(line);
            } else {
                contains.add(line);
            }
        }
    }

}
