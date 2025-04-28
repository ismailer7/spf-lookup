package com.spf.lookup;

import com.spf.utils.DomainUtils;
import com.spf.utils.FileUtils;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpfChecker {

    public static final String RESET = "\033[0m";
    public static final String GREEN = "\033[0;32m";
    public static final String YELLOW = "\033[0;33m";
    public static final String RED = "\033[0;31m";
    public static final String BLUE = "\033[0;34m";

    private final Set<String> checkedDomains = new HashSet<>(); // Avoid infinite loops

    public SpfChecker() {}

    public void check(String rootDomain, File result, File checked, File origin, File logFile, IgnoredDomainList ignoredDomainList, File guideFile, File guideFile2) throws IOException, InterruptedException {
        var spfSimplify = new StringBuilder();
        spfSimplify.append(rootDomain);

        var spfSimplify2 = new StringBuilder();
        spfSimplify2.append(rootDomain);

        Pattern pattern = Pattern.compile("(include|exists|redirect)(:|=)([a-zA-Z0-9\\._-]*)");

        Queue<String> queue = new LinkedList<>();
        queue.add(rootDomain);

        while (!queue.isEmpty()) {
            String domain = queue.poll();
            if (checkedDomains.contains(domain)) continue; // Skip already processed domains
            checkedDomains.add(domain);

            //System.out.println(BLUE + "[INFO] Checking SPF for domain: " + domain + RESET);

            try {
                // Setup DNS context
                var env = new Hashtable<String, String>();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
                DirContext ctx = new InitialDirContext(env);
                Attributes attrs = ctx.getAttributes(domain, new String[]{"TXT"});

                var txtAttr = attrs.get("TXT");
                if (txtAttr == null) {
                    //System.out.println(RED + "[WARN] No SPF record found for " + domain + RESET);
                    FileUtils.write(checked, domain);
                    continue;
                }

                NamingEnumeration<?> txtRecords = txtAttr.getAll();

                while (txtRecords.hasMore()) {
                    var record = (String) txtRecords.next();
                    record = record.replaceAll("\"\\s*\"", "");

                    Matcher matcher = pattern.matcher(record);
                    FileUtils.write(origin, domain + " >>> " + record);

                    if (record.startsWith("\"v=spf1")) {
                        //System.out.println(YELLOW + "[SPF] Found SPF record for " + domain + ": " + record + RESET);

                        while (matcher.find()) {
                            var subDomain = matcher.group(3).trim();
                            if (!subDomain.isEmpty() && !checkedDomains.contains(subDomain) && !ignoredDomainList.isIgnored(subDomain)) {
                                FileUtils.write(result, subDomain);
                                spfSimplify.append(" --- ").append(subDomain);
                                spfSimplify2.append(" --- ").append(DomainUtils.getRootDomain(subDomain));
                                //System.out.println(GREEN + "[INCLUDE] Queued: " + subDomain + RESET);
                                queue.add(subDomain); // Add to queue instead of recursion
                            }
                        }
                    }
                }

            } catch (NamingException e) {
                //System.err.println(RED + "[ERROR] Could not retrieve SPF record for: " + domain + RESET);
                FileUtils.write(logFile, "Error retrieving SPF record for domain: " + domain);
            }
        }
        spfSimplify.append("\n");
        var spfSiplifyString = spfSimplify.toString();
        var spfSiplifyString2 = spfSimplify2.toString();
        FileUtils.write(guideFile, spfSiplifyString);
        FileUtils.write(guideFile2, spfSiplifyString2);
    }
}
