//
// ListRecords.java --- E3DB example code to list records.
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

package com.tozny.e3db.examples;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

import com.memoizrlabs.retrooptional.Optional;
import com.google.gson.*;

import com.tozny.e3db.client.*;

/**
 * Example code using the Tozny E3DB API to create a client and
 * list accessible records.
 *
 * This client then filters records to look for "feedback" comments
 * and lists the feedback that has been shared with this CLIENT_ID.
 *
 * The user must supply the client ID, API key ID, and API secret
 * on the command line. These values are obtained during registration
 * (when running `e3db register') and can be displayed by running
 * `e3db info'.
 */
public class ListRecords {

  static class Config {
    UUID clientId;
    String apiKeyId;
    String apiSecret;
    String apiUrl;
  }

  public static Config getConfig (String[] args) throws FileNotFoundException {
    Config config = new Config();
    if (args.length == 3) {
      System.out.println ("Reading configuration from: Command line.");
      config.clientId  = UUID.fromString(args[0]);
      config.apiKeyId  = args[1];
      config.apiSecret = args[2];
      config.apiUrl    = "https://api.e3db.tozny.com/v1";
    } else { //Read from config file.



      Path configFile = Paths.get(System.getProperty("user.home"), ".tozny", "e3db.json");

      BufferedReader br = new BufferedReader(new FileReader(configFile.toString()));
      JsonParser parser = new JsonParser();
      JsonObject jsonConfig = parser.parse(br).getAsJsonObject();

      System.out.println ("Reading configuration from: " + configFile);
      config.clientId = UUID.fromString(jsonConfig.get("client_id").getAsString());
      config.apiKeyId = jsonConfig.get("api_key_id").getAsString();
      config.apiSecret = jsonConfig.get("api_secret").getAsString();
      config.apiUrl = jsonConfig.get("api_url").getAsString();
    }
    return config;
  }

  public static void main(String[] args) {
    Config config = null;
    try {
      config = getConfig(args);
    } catch (Exception e) {
      config = null;
    }
    if (config == null) {
      System.err.println ("Could not read configuration from file or command line.");
      System.err.println("Usage: ListRecords CLIENT_ID API_KEY_ID API_SECRET");
      System.exit(1);
    }

    try {
      KeyManager keyManager = ConfigFileKeyManager.get();
      Client client = new HttpE3DBClientBuilder()
        .setClientId(config.clientId)
        .setApiKeyId(config.apiKeyId)
        .setApiSecret(config.apiSecret)
        .setKeyManager(keyManager)
        .setServiceUri(config.apiUrl)
        .build();

      // Write an encrypted record of type "feedback":
      Meta writeMeta = new Meta(config.clientId, config.clientId, "feedback");
      HashMap <String, String> map = new HashMap();
      map.put("comment", "Hello World! I successfully ran the example file.");
      Record feedbackRecord1 = new Record(writeMeta, map);
      UUID feedbackRecordId = client.writeRecord(feedbackRecord1);
      System.out.println("Feedback Created: " + feedbackRecordId);

      //Read back the record we just wrote to the database
      Record feedbackRecord2 = client.readRecord(feedbackRecordId).get();
      System.out.println ("Read the comment: " + feedbackRecord2.data.get("comment"));

      //Share "feedback" records with Isaac
      UUID ipjId = UUID.fromString ("166ed61a-3a56-4fe6-980f-2850aa82ea25"); // IPJ!!
      client.authorizeReader(config.clientId, ipjId, "feedback");
      PolicyRequest shareReq = new PolicyRequest(config.clientId,
              config.clientId,
              ipjId,
              Policy.allow(Policy.READ),
              "feedback"
      );
      client.setPolicy(shareReq);

      // Print out all the records:
      for (Meta meta : client.listRecords(100, 0)) {
          System.out.printf("%-40s %s\n", meta.record_id, meta.type);
      }

      // Now do the same, but just show the feedback comments:
      List<String> feedbackType = new ArrayList<String>();
      feedbackType.add("feedback");

      System.out.println ("\nUser ID                                    Comment");
      System.out.println ("--------------------------------------------------");
      for (Meta meta : client.listRecords(100, 0, feedbackType)) {
        Optional<Record> maybeRecord = client.readRecord (meta.record_id);
        if (maybeRecord.isPresent()) {
          Record r = maybeRecord.get();
          String comment = r.data.get("comment");
          if (comment != null) {
            System.out.println(meta.writer_id + " says: " + comment);

            try { // Share a "thank you" record with that client.
              client.authorizeReader (config.clientId, meta.writer_id, "tozny_says_thanks");
              PolicyRequest req = new PolicyRequest(config.clientId,
                  config.clientId,
                  meta.writer_id,
                  Policy.allow(Policy.READ),
                  "tozny_says_thanks"
              );
              client.setPolicy(req);

            } catch (java.util.NoSuchElementException e) {}
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
