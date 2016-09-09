//
// main.scala --- PDS CLI main program.
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

package com.tozny.pds.cli

import java.io.File
import java.util.UUID
import java.util.prefs._

import scala.io.StdIn
import scala.collection.JavaConversions._
import scalaz.concurrent.Task

import org.jose4j.jwe._
import org.jose4j.jwx._
import org.jose4j.keys.PbkdfKey
import org.jose4j.lang.ByteUtil

import com.tozny.pds.client.PDSClient

object Main extends App {
  /** Length in bytes of the user's root AES key. */
  private val ROOT_KEY_LENGTH_BYTES = 32

  /** Initialize the configuration file interactively. */
  private def initConfig(opts: Options): Config = {
    val defaults = Config.load(opts.config_file).getOrElse(Config.defaults)
    val config = Config.init(defaults)
    Config.save(opts.config_file, config)
    config
  }

  /** Load the configuration, initializing if necessary. */
  private def loadConfig(opts: Options): Config = {
    if (opts.command == Init) {
      initConfig(opts)
      sys.exit(0)
    } else {
      Config.load(opts.config_file) match {
        case Some(x) => x
        case None => initConfig(opts)
      }
    }
  }

  /** Process the requested command, returning a task to run. */
  private def run(opts: Options, client: PDSClient) =
    opts.command match {
      case Ls(limit, offset) => {
        val records = client.listRecords(limit, offset).toList

        printf("%-40s  %-12s  %s\n", "Record ID", "Producer", "Type")
        println("-" * 78)
        records.foreach { rec =>
          printf("%-40s  %-12s  %s\n", rec.record_id.get,
                 rec.producer_id.toString.slice(0, 8) + "...", rec.`type`)
        }
      }

      case Read(dest, recordId) => {
        println(client.readRecord(recordId))
      }

      case Write(ctype, data) => {
        // Load data from file or the supplied string.
        // Convert data to a JSON record.               \
        // Encrypt the data and wrap it in metadata.    |- should be done by SDK?
        // Submit the wrapped data to the PDS service.  /
      }

      case Init => {}   // handled in main
    }

  // Parse arguments and load configuration.
  val opts = OptionParser.parse(args)
  val config = loadConfig(opts)

  // Create the PDS REST client and execute the requested command,
  // returning a Task to be run.
  val client = new PDSClient.Builder()
    .setServiceUri(config.api_url)
    .setApiKeyId(config.api_key_id)
    .setApiSecret(config.api_secret)
    .build()

  try {
    run(opts, client)
  } catch {
    case e: Exception => {
      println(s"Error: ${e.getMessage}")
    }
  }
}
