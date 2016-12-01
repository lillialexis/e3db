//
// ListRecords.java --- E3DB example code to list records.
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

package com.tozny.e3db.examples;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.memoizrlabs.retrooptional.Optional;
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
  public static void main(String[] args) {
    if (args.length != 3) {
      System.err.println("Usage: ListRecords CLIENT_ID API_KEY_ID API_SECRET");
      System.exit(1);
    }

    try {
      UUID clientId = UUID.fromString(args[0]);
      String apiKeyId = args[1];
      String apiSecret = args[2];
      KeyManager keyManager = ConfigFileKeyManager.get();
      Client client = new HttpE3DBClientBuilder()
        .setClientId(clientId)
        .setApiKeyId(apiKeyId)
        .setApiSecret(apiSecret)
        .setKeyManager(keyManager)
        .setServiceUri("https://api.e3db.tozny.com/v1")
        .build();

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
              client.authorizeReader (clientId, meta.writer_id, "tozny_says_thanks");
              PolicyRequest req = new PolicyRequest(clientId,
                  clientId,
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
