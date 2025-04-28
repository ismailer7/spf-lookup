package com.spf.licence;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.spf.config.ConfigLoader;
import org.bson.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LicenceValidator {

    public static boolean validateLicense(String licenseKey) {
        try (MongoClient mongoClient = MongoClients.create(ConfigLoader.get("mongo.uri"))) {
            MongoDatabase db = mongoClient.getDatabase(ConfigLoader.get("mongo.database"));
            MongoCollection<Document> licenses = db.getCollection(ConfigLoader.get("mongo.collection"));

            Document query = new Document("license_key", licenseKey);
            Document license = licenses.find(query).first();

            if (license == null) {
                System.out.println("❌ License not found.");
                return false;
            }
            Document updateFields = new Document()
                    .append("executions", new Document("$inc", 1))
                    .append("last_execution", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

            licenses.updateOne(
                    Filters.eq("license_key", licenseKey),
                    new Document("$set", new Document("last_execution", updateFields.getString("last_execution")))
                            .append("$inc", new Document("execution", 1))
            );

            if (!license.getBoolean("active", false)) {
                System.out.println("❌ License is not active.");
                return false;
            }

            String expiresAt = license.getString("expires_at");
            LocalDate expiryDate = LocalDate.parse(expiresAt.substring(0, 10), DateTimeFormatter.ISO_DATE);
            if (expiryDate.isBefore(LocalDate.now())) {
                System.out.println("❌ License has expired.");
                return false;
            }

            //System.out.println("✅ License is valid for " + license.getString("customer_name"));
            return true;

        } catch (Exception e) {
            //System.out.println("❌ Error validating license: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
