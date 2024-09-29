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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpfChecker {

    public static final String RESET = "\033[0m";
    public static final String GREEN = "\033[0;32m";

    public static final String YELLOW = "\033[0;33m";

    private String domain;

    public SpfChecker(String domain) {
        this.domain = domain;
        System.out.println("SPF Check for Domain " + YELLOW + domain + RESET);
    }

    synchronized void check(String domain, int deep, File result, File checked) {
        try {
            Pattern pattern = Pattern.compile("(include|exists|redirect)(:|=)[\\w{}%\\.]*");
            Matcher matcher = null;

            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"TXT"});
            NamingEnumeration<?> txtRecords = null;
            var txt = attrs.get("TXT");
            if(txt == null) {
                FileUtils.write(checked, domain);
                return;
            } else {
                txtRecords = attrs.get("TXT").getAll();
            }

            while (txtRecords.hasMore()) {
                String record = (String) txtRecords.next();
                matcher = pattern.matcher(record);
                if (record.startsWith("\"v=spf1")) {
                    //System.out.println(tabs(deep) + "> SPF record for " + domain + ": " + record);
                    while(matcher.find()) {
                        //System.out.println(tabs(deep + 1) + "> " + GREEN + matcher.group() + RESET);
                        FileUtils.write(result, matcher.group().replaceAll("(include|exists|redirect)(:|=)", ""));
                        check(matcher.group().replace("include:", ""), deep + 2, result, checked);
                    }

                    return;
                }
            }

            //System.out.println("No SPF record found for " + domain);
        } catch (NamingException e) {
            // System.out.println("Error retrieving SPF record: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }


    String tabs(int deep) {
        return "\t".repeat(Math.max(0, deep));
    }

}
