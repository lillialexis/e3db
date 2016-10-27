//
// ListRecords.java --- E3DB example code to list records.
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

package com.tozny.pds.examples;

import java.util.List;
import java.util.UUID;

import com.tozny.pds.client.PDSClient;
import com.tozny.pds.client.KeyManager;
import com.tozny.pds.client.Meta;
import com.tozny.pds.client.ConfigFileKeyManager;

/**
 * Example code using the Tozny PDS API to create a client and
 * list accessible records.
 *
 * The user must supply the client ID, API key ID, and API secret
 * on the command line. These values are obtained during registration
 * (when running `pds register') and can be displayed by running
 * `pds info'.
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
      PDSClient client = new PDSClient.Builder()
        .setClientId(clientId)
        .setApiKeyId(apiKeyId)
        .setApiSecret(apiSecret)
        .setKeyManager(keyManager)
        .setServiceUri("https://api.dev.pds.tozny.com/v1")
        .build();

      for (Meta meta : client.listRecords(100, 0)) {
        System.out.printf("%-40s %s\n", meta.record_id, meta.type);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
