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
import scalaz.syntax.either._

import argonaut.JsonParser

import com.tozny.pds.client._

object Main {
  private val ok = ().right

  /** State passed to each command handler. */
  case class State(opts: Options, config: Config, client: PDSClient)

  /** List records accessible to this client. */
  private def do_ls(state: State, cmd: Ls): CLIError \/ Unit = {
    val records = state.client.listRecords(cmd.limit, cmd.offset).toList

    printf("%-40s  %-12s  %s\n", "Record ID", "Producer", "Type")
    println("-" * 78)
    records.foreach { rec =>
      printf("%-40s  %-12s  %s\n", rec.record_id.get,
             rec.producer_id.toString.slice(0, 8) + "...", rec.`type`)
    }

    ok
  }

  /** Read a record by ID. */
  private def do_read(state: State, cmd: Read): CLIError \/ Unit = {
    println(state.client.readRecord(cmd.record_id))
    ok
  }

  /** Write a record given type and data. */
  private def do_write(state: State, cmd: Write): CLIError \/ Unit = {
    val data_opt = cmd.data.list.toList.mkString(" ")
    val data =
      if (data_opt.charAt(0) == '@') {
        new String(Files.readAllBytes(Paths.get(data_opt.substring(1))))
      } else {
        data_opt
      }

    val obj = JsonParser.parse(data) match {
      case Left(err) => {
        return DataError(s"Invalid data: ${err}").left
      }
      case Right(x) => x
    }

    val meta = new Meta(None, state.config.client_id,
      cmd.user_id.getOrElse(state.config.client_id), cmd.ctype, None, None)
    val dataMap = obj.as[Map[String, String]].value.get
    val record = new Record(meta, dataMap)
    val record_id = state.client.writeRecord(record)

    println(record_id)
    ok
  }

  /** Process the requested command, synchronously. */
  private def run(opts: Options, client: PDSClient, config: Config): CLIError \/ Unit = {
    val state = State(opts, config, client)

    opts.command match {
      case cmd @ Ls(_,_)        => do_ls(state, cmd)
      case cmd @ Read(_, _)     => do_read(state, cmd)
      case cmd @ Write(_, _, _) => do_write(state, cmd)
      case       Register       => ok
    }
  }

  // For future use in register command:
  //
  // private def readLineDefault(prompt: String, default: String): String = {
  //   val value = StdIn.readLine(s"${prompt} [${default}]: ")
  //   if (value == "") { default } else { value }
  // }
  //
  // /** Initialize a configuration interactively from a set of defaults. */
  // def init(defaults: Config): Config = {
  //   val api_url = readLineDefault("API URL", defaults.api_url)
  //   val api_key_id = readLineDefault("API Key ID", defaults.api_key_id)
  //   val api_secret = readLineDefault("API Secret", defaults.api_secret)
  //   val client_id = readLineDefault("Client UUID", defaults.client_id.toString)
  //   val client_key =
  //     if (defaults.client_key.isEmpty) {
  //       Some(RsaJwkGenerator.generateJwk(KEY_PAIR_BITS))
  //     } else {
  //       defaults.client_key
  //     }
  //
  //   Config(VERSION, client_key, UUID.fromString(client_id),
  //          api_url, api_key_id, api_secret)
  // }

  def main(args: Array[String]) {
    // Parse arguments and load configuration.
    val opts = OptionParser.parse(args)

    // Short-circuit to registration before loading the config.
    if (opts.command == Register) {
      println("Doing registration flow.")
      sys.exit(0)
    }

    // Create the PDS REST client and execute the requested command.
    val result = for {
      config <- Config.load(opts.config_file)
      client = new PDSClient.Builder()
        .setServiceUri(config.api_url)
        .setApiKeyId(config.api_key_id)
        .setApiSecret(config.api_secret)
        .build()
      res <- run(opts, client, config)
    } yield res

    result.leftMap { err =>
      println(err.message)
    }
  }
}
