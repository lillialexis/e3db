//
// main.scala --- PDS CLI main program.
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

package com.tozny.pds.cli

import java.nio.file.{Files,Paths}
import scala.collection.JavaConversions._
import scalaz._
import argonaut.JsonParser

import com.tozny.pds.client._

object Main extends App {
  /** Initialize the configuration file interactively. */
  private def initConfig(opts: Options): Config = {
    val defaults = Config.load(opts.config_file).getOrElse(Config.defaults)
    val config = Config.init(defaults)
    Config.save(opts.config_file, config)
    config
  }

  /** Load the configuration, initializing if necessary. */
  private def loadConfig(opts: Options): Config = {
    Config.load(opts.config_file) match {
      case Some(x) => x
      case None => initConfig(opts)
    }
  }

  /** Process the requested command, synchronously. */
  private def run(opts: Options, client: PDSClient, config: Config) =
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

      case Write(user_id, ctype, data_opts) => {
        val data_opt = data_opts.list.toList.mkString(" ")
        val data =
          if (data_opt.charAt(0) == '@') {
            new String(Files.readAllBytes(Paths.get(data_opt.substring(1))))
          } else {
            data_opt
          }

        val obj = JsonParser.parse(data) match {
          case Left(err) => {
            println(s"Invalid data: ${err}")
            sys.exit(1)
          }
          case Right(x) => x
        }

        val meta = new Meta(None, config.client_id,
          user_id.getOrElse(config.client_id), ctype, None, None)
        val dataMap = obj.as[Map[String, String]].value.get
        val record = new Record(meta, dataMap)
        val record_id = client.writeRecord(record)

        println(record_id)
      }

      case Register => {}   // handled in main
    }

  // Parse arguments and load configuration.
  val opts = OptionParser.parse(args)
  val config = loadConfig(opts)

  // Create the PDS REST client and execute the requested command.
  val client = new PDSClient.Builder()
    .setServiceUri(config.api_url)
    .setApiKeyId(config.api_key_id)
    .setApiSecret(config.api_secret)
    .build()

  try {
    run(opts, client, config)
  } catch {
    case e: Exception => {
      println(s"Error: ${e.getMessage}")
    }
  }
}
