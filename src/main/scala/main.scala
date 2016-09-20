//
// main.scala --- PDS CLI main program.
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

package com.tozny.pds.cli

import java.nio.file.{Files,Paths}

import scala.annotation.tailrec
import scala.io.StdIn
import scala.collection.JavaConversions._

import scalaz._
import scalaz.syntax.either._

import argonaut.JsonParser

import org.jose4j.jwk._

import com.tozny.pds.client._

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

  private val DEFAULT_SERVICE_URL = "https://api.pds.tozny.com/v1"
  private val KEY_PAIR_BITS = 4096
  private val ACCESS_KEY_BITS = 256

  /** Perform interactive registration and exit. */
  private def do_register(opts: Options): CLIError \/ Unit = {
    val email = readLineRequired("E-Mail Address", "")
    val url = readLineDefault("Service URL", DEFAULT_SERVICE_URL)
    val client = new PDSClient.Builder().setServiceUri(url).build()
    val client_key = RsaJwkGenerator.generateJwk(KEY_PAIR_BITS)
    val access_key = OctJwkGenerator.generateJwk(ACCESS_KEY_BITS)

    val req = RegisterRequest(email, client_key)
    val resp = client.register(req)
    val config = Config(client_key, access_key, resp.client_id, url,
                        resp.api_key_id, resp.api_secret)

    Config.save(opts.config_file, config)
  }

  /** List records accessible to this client. */
  private def do_ls(state: State, cmd: Ls): CLIError \/ Unit = {
    val records = state.client.listRecords(cmd.limit, cmd.offset).toList

    printf("%-40s  %-12s  %s\n", "Record ID", "Producer", "Type")
    println("-" * 78)
    records.foreach { rec =>
      printf("%-40s  %-12s  %s\n", rec.record_id.get,
             rec.writer_id.toString.slice(0, 8) + "...", rec.`type`)
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

  private def mainOpts(opts: Options): CLIError \/ Unit = {
    if (opts.command == Register) {
      do_register(opts)
    } else {
      for {
        config <- Config.load(opts.config_file)
        client = new PDSClient.Builder()
          .setServiceUri(config.api_url)
          .setApiKeyId(config.api_key_id)
          .setApiSecret(config.api_secret)
          .build()
        res <- run(opts, client, config)
      } yield res
    }
  }

  def main(args: Array[String]) {
    CLIError.handle {
      mainOpts(OptionParser.parse(args))
    }.leftMap {
      case err: ConfigError => {
        println(err.message)
        println("Run `pds register' to create an account.")
      }

      case err => {
        println(err.message)
      }
    }
  }
}
