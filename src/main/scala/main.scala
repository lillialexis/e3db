//
// main.scala --- PDS CLI main program.
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

package com.tozny.pds.cli

import java.nio.file.{Files, Paths}

import scala.annotation.tailrec
import scala.io.StdIn
import scala.collection.JavaConversions._
import scalaz._
import scalaz.syntax.either._
import argonaut.JsonParser
import org.jose4j.jwk._
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers

import com.tozny.pds.client_java._

object Main {
  private val ok = ().right

  /** State passed to each command handler. */
  case class State(opts: Options, config: Config, client: PDSClient)

  /** Read an interactive value with a default. */
  private def readLineDefault(prompt: String, default: String): String = {
    val value = StdIn.readLine(s"${prompt} [${default}]: ")
    if (value == "") { default } else { value }
  }

  /** Read a required interactive value with a default. */
  @tailrec
  private def readLineRequired(prompt: String, default: String): String = {
    val value = readLineDefault(prompt, default)

    if (value == "") {
      println("A value is required for this field.")
      readLineRequired(prompt, default)
    } else {
      value
    }
  }

  private val DEFAULT_SERVICE_URL = "https://api.dev.pds.tozny.com/v1"
  private val KEY_PAIR_BITS = 4096
  private val ACCESS_KEY_BITS = 256
  private val CAB_VERSION = "1.0"

  /** Perform interactive registration and exit. */
  private def do_register(opts: Options): CLIError \/ Unit = {
    val email = readLineRequired("E-Mail Address", "")
    val url = readLineDefault("Service URL", DEFAULT_SERVICE_URL)
    var client = new PDSClient.Builder().setServiceUri(url).build()

    client_key.setAlgorithm(KeyManagementAlgorithmIdentifiers.RSA1_5)
    val req = new RegisterRequest(email, client_key)
    val resp = client.register(req)
    val config = Config(resp.client_id, url, resp.api_key_id, resp.api_secret)

    Config.save(opts.config_file, config)

    // Create a new client with credentials so we can put an initial CAB.
    client = new PDSClient.Builder()
      .setServiceUri(config.api_url)
      .setApiKeyId(config.api_key_id)
      .setApiSecret(config.api_secret)
      .setKeyManager(mgr)
      .build()

    // Create an initial CAB with a single entry for the writer.
    val id = resp.client_id
    // user == writer == authorizer (for now)
    val cab = new Cab(CAB_VERSION, new Cab.Id(id), new java.util.ArrayList(),
                      new Cab.Id(id), new Cab.Id(id))
    client.putCab(id, id, cab)

    ok
  }

  /** Display configuration information. */
  private def do_info(state: State): CLIError \/ Unit = {
    printf("%-20s %s\n", "Client ID:", state.config.client_id)
    ok
  }

  /** List records accessible to this client. */
  private def do_ls(state: State, cmd: Ls): CLIError \/ Unit = {
    val records = state.client.listRecords(cmd.limit, cmd.offset).toList

    printf("%-40s  %-12s  %s\n", "Record ID", "Producer", "Type")
    println("-" * 78)
    records.foreach { rec =>
      printf("%-40s  %-12s  %s\n", rec.record_id,
             rec.writer_id.toString.slice(0, 8) + "...", rec.`type`)
    }

    ok
  }

  /** Read a record by ID. */
  private def do_read(state: State, cmd: Read): CLIError \/ Unit = {
    val rec =
      if (cmd.raw) {
        state.client.readRawRecord(cmd.record_id)
      } else {
        state.client.readRecord(cmd.record_id)
      }

    printf("%-20s %s\n", "Record ID:", rec.meta.record_id)
    printf("%-20s %s\n", "Record Type:", rec.meta.`type`)
    printf("%-20s %s\n", "Writer ID:", rec.meta.writer_id)
    printf("%-20s %s\n", "User ID:", rec.meta.user_id)
    printf("\n")
    printf("%-20s %s\n", "Field", "Value")
    println("-" * 78)

    rec.data.foreach { case (k, v) =>
      printf("%-20s %s\n", k, v)
    }

    ok
  }

  private def do_share(state: State, cmd: Sharing with Command): CLIError \/ Unit = {
    val req = cmd match {
      case AddSharing(reader, content_type) => {
        val user_id = state.config.client_id    // assume writer == user for now
        state.client.authorizeReader(user_id, reader)
        new PolicyRequest(state.config.client_id,
          state.config.client_id,
          reader,
          Policy.allow(Policy.READ),
          content_type
        )
      }
      case RemoveSharing(reader, content_type) => {
        new PolicyRequest(state.config.client_id,
          state.config.client_id,
          reader,
          Policy.deny(Policy.READ),
          content_type
        )
      }
    }

    state.client.setPolicy(req)
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

    JsonParser.parse(data) match {
      case Left(err) => {
        DataError(s"Invalid data: ${err}").left
      }
      case Right(obj) => {
        val meta = new Meta(None, state.config.client_id,
          cmd.user_id.getOrElse(state.config.client_id), cmd.ctype, None, None)
        val dataMap = obj.as[Map[String, String]].value.get
        val record = new Record(meta, dataMap)
        val record_id = state.client.writeRecord(record)

        println(record_id)
        ok
      }
    }

    val meta = new Meta(null, state.config.client_id,
      cmd.user_id.getOrElse(state.config.client_id), cmd.ctype, null, null)
    val dataMap = obj.as[Map[String, String]].value.get
    val record = new Record(meta, dataMap)
    val record_id = state.client.writeRecord(record)

    println(record_id)
    ok
  }

  /** Read a CAB from the PDS and print it. */
  private def do_getcab(state: State, cmd: GetCab): CLIError \/ Unit = {
    val cab = state.client.getCab(cmd.writer_id, cmd.user_id)

    printf("%-20s %s\n", "CAB Version:", cab.version)
    printf("%-20s %s\n", "Authorizer ID:", cab.authorizer.id)
    printf("%-20s %s\n", "Writer ID:", cab.writer.id)
    printf("%-20s %s\n", "User ID:", cab.user.id)

    printf("\n")
    printf("%-40s %s\n", "Reader ID", "Encrypted Access Key")
    println("-" * 78)

    cab.pairs.foreach { pair =>
      printf("%-40s %s\n", pair.reader_id, pair.eak)
    }

    ok
  }

  /** Read a client's public key from the PDS and print it. */
  private def do_getkey(state: State, cmd: GetKey): CLIError \/ Unit = {
    val keyJson = state.client.getClientKey(cmd.client_id).toJson
    \/.fromEither(JsonParser.parse(keyJson)).map { obj =>
      println(obj.spaces2)
    }.leftMap(DataError)
  }

  /** Process the requested command, synchronously. */
  private def run(opts: Options, client: PDSClient, config: Config): CLIError \/ Unit = {
    val state = State(opts, config, client)

    opts.command match {
      case cmd : Ls => do_ls(state, cmd)
      case cmd : Read => do_read(state, cmd)
      case cmd : Write => do_write(state, cmd)
      case cmd : Sharing => do_share(state, cmd)
      case cmd : GetCab => do_getcab(state, cmd)
      case cmd : GetKey => do_getkey(state, cmd)
      case       Info => do_info(state)
      case       Register       => ok
    }
  }

  private def mainOpts(opts: Options): CLIError \/ Unit = {
    if (opts.command == Register) {
      do_register(opts)
    } else {
      for {
        config <- Config.load(opts.config_file)
        client = new PDSClient.Builder()
          .setClientId(config.client_id)
          .setServiceUri(config.api_url)
          .setApiKeyId(config.api_key_id)
          .setApiSecret(config.api_secret)
          .setKeyManager(ConfigFileKeyManager.get())
          .build()
        res <- run(opts, client, config)
      } yield res
    }
  }

  def main(args: Array[String]) {
    (try {
      mainOpts(OptionParser.parse(args))
    } catch {
      case e: PDSClientError => ClientError(e.getMessage, e).left
      case e: Exception      => MiscError(e.getMessage, e).left
    }).leftMap {
      case err: ConfigError => {
        println(err.message)
        println("Run `pds register' to create an account.")
      }

      case err: MiscError => {
        err.exception.printStackTrace()
      }

      case err => {
        println(err.message)
      }
    }
  }
}
