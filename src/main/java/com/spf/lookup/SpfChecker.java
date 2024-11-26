package com.spf.lookup;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpfChecker {

    public static final String RESET = "\033[0m";
    public static final String GREEN = "\033[0;32m";

    public static final String YELLOW = "\033[0;33m";

    private String domain;

    private final List<String> unawanted = List.of("");

    public SpfChecker(String domain) {
        this.domain = domain;
        System.out.println("SPF Check for Domain " + YELLOW + domain + RESET);
    }

    synchronized void check(String domain, int deep, File result, File checked, File origin, File logFile) throws IOException, InterruptedException {
        try {
            Pattern pattern = Pattern.compile("(include|exists|redirect)(:|=)([a-zA-Z0-9\\._-]*)");
            Matcher matcher = null;

            var env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"TXT"});
            NamingEnumeration<?> txtRecords = null;
            var txt = attrs.get("TXT");

            if(txt == null) {
                if(deep == 0) {
                    FileUtils.write(checked, domain);
                    return;
                }
            } else {
                txtRecords = attrs.get("TXT").getAll();
            }

            while (txtRecords != null && txtRecords.hasMore()) {
                var record = (String) txtRecords.next();
                record = record.replaceAll("\"\\s*\"", "");
                matcher = pattern.matcher(record);
                FileUtils.write(origin, domain + " >>> " + record);
                if (record.startsWith("\"v=spf1")) {
                    //System.out.println(tabs(deep) + "> SPF record for " + domain + ": " + record);
                    while(matcher.find()) {
                        //System.out.println(tabs(deep + 1) + "> " + GREEN + matcher.group() + RESET);
                        var dom = matcher.group(3).trim();
                        if(!dom.isEmpty()) {
                            FileUtils.write(result, matcher.group(3));
                            var newDomain = matcher.group().replaceAll("(include|redirect|exists):", "");
                            if(!newDomain.equals(domain)) {
                                check(newDomain, deep + 2, result, checked, origin, logFile);
                            }
                        }
                    }

                    return;
                }
            }

            //System.out.println("No SPF record found for " + domain);
        } catch (NamingException e) {
             //System.out.println("Error retrieving SPF record for domain: " + domain);
             FileUtils.write(logFile, String.format("Error retrieving SPF record for domain: %s", domain));
             return;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
