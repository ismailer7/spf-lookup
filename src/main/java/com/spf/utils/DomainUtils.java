package com.spf.utils;
import com.google.common.net.*;

public class DomainUtils {

    public static String getRootDomain(String fullDomain) {
        try {
            InternetDomainName domain = InternetDomainName.from(fullDomain);
            if (domain.hasPublicSuffix() && domain.isUnderPublicSuffix()) {
                return domain.topPrivateDomain().toString();
            }
        } catch (IllegalArgumentException e) { }
        return fullDomain; // fallback
    }

}
