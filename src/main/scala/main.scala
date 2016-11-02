//
// main.scala --- E3DB CLI main program.
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

package com.tozny.e3db.cli

import java.io.FileOutputStream
import java.nio.file.{Files, Paths}
import javax.activation.MimetypesFileTypeMap
import java.util.UUID

import scala.annotation.tailrec
import scala.io.StdIn
import scala.collection.JavaConversions._

import scalaz._
import scalaz.syntax.either._
import argonaut.JsonParser
import org.jose4j.jwk._
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers

import com.tozny.e3db.client._
import org.jose4j.base64url.Base64
import org.jose4j.jwe._

import scala.util.parsing.json.JSONObject

object Main {
  private val ok = ().right

  private implicit class OptSyntax[A](opt: java.util.Optional[A]) {
    def asScala(): Option[A] = {
      if (opt != null && opt.isPresent) Option(opt.get)
      else None
    }
  }

  /** State passed to each command handler. */
  case class State(opts: Options, config: Config, client: Client)

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

  private val DEFAULT_SERVICE_URL = "https://api.e3db.tozny.com/v1"
  private val KEY_PAIR_BITS = 4096
  private val CAB_VERSION = "1.0"

  /** Perform interactive registration and exit. */
  private def do_register(opts: Options): CLIError \/ Unit = {
    val email = readLineRequired("E-Mail Address", "")
    val url = readLineDefault("Service URL", DEFAULT_SERVICE_URL)
    val mgr = ConfigFileKeyManager.create(opts.config_file.getParent.resolve("e3db_key.json").toFile)
    val resp = new Registration.Builder()
      .setServiceUri(url)
      .setEmail(email)
      .setPublicKey(PublicJsonWebKey.Factory.newPublicJwk(mgr.getPublicKey))
      .build()
      .register()

    val config = Config(resp.client_id, url, resp.api_key_id, resp.api_secret)

    Config.save(opts.config_file, config)

    ok
  }

  /** Display configuration information. */
  private def do_info(state: State): CLIError \/ Unit = {
    println(f"${"Client ID:"}%-20s ${state.config.client_id}")
    ok
  }

  /** List records accessible to this client. */
  private def do_ls(state: State, cmd: Ls): CLIError \/ Unit = {
    val records = state.client.listRecords(cmd.limit, cmd.offset).toList

    println(f"${"Record ID"}%-40s  ${"Producer"}%-12s  ${"Type"}")
    println("-" * 78)
    records.foreach { rec =>
      println(f"${rec.record_id}%-40s  ${rec.writer_id.toString.slice(0, 8) + "..."}%-12s  ${rec.`type`}")
    }

    ok
  }

  /** Read a record by ID. */
  private def do_read(state: State, cmd: Read): CLIError \/ Unit = {
    if (cmd.raw) {
      do_raw_read(state, cmd)
    } else {
      state.client.readRecord(cmd.record_id).asScala.map({ rec =>

        if (cmd.dest.isDefined) {
          val stream = new FileOutputStream(cmd.dest.get)
          try {
            stream.write(JSONObject(rec.data.toMap).toString().getBytes())
          } finally {
            stream.close()
          }
        } else {
          println(f"${"Record ID:"}%-20s ${rec.meta.record_id}")
          println(f"${"Record Type:"}%-20s ${rec.meta.`type`}")
          println(f"${"Writer ID:"}%-20s ${rec.meta.writer_id}")
          println(f"${"User ID:"}%-20s ${rec.meta.user_id}")
          println("")
          println(f"${"Field"}%-20s Value")
          println("-" * 78)

          rec.data.foreach { case (k, v) =>
            println(f"${k}%-20s ${v}")
          }
        }
      }).getOrElse({
        println(f"${"Record ID:"}%-20s ${cmd.record_id} not found.")
      })

      ok
    }
  }

  /** Read a record by ID without decrypting. */
  private def do_raw_read(state: State, cmd: Read): CLIError \/ Unit = {
    val rec = state.client.readRawRecord(cmd.record_id).asScala.map({ rec =>

      println(f"${"Record ID:"}%-20s ${rec.meta.record_id}")
      println(f"${"Record Type:"}%-20s ${rec.meta.`type`}")
      println(f"${"Writer ID:"}%-20s ${rec.meta.writer_id}")
      println(f"${"User ID:"}%-20s ${rec.meta.user_id}")
      println("")
      println(f"${"Field"}%-20s Value")
      println("-" * 78)

      rec.data.foreach { case (k, v) =>
        println(f"${k}%-20s ${v}")
      }
    }).getOrElse(
      println(f"${"Record ID:"}%-20s ${cmd.record_id} not found.")
    )
    ok
  }

  /** Read a file by ID */
  private def do_readfile(state: State, cmd: ReadFile): CLIError \/ Unit = {
    state.client.readRecord(cmd.record_id).asScala.map({ rec =>
      val filename = cmd.dest.getOrElse(Paths.get(rec.data("filename")).getFileName.toString)
      val stream = new FileOutputStream(filename)
      try {
        stream.write(Base64.decode(rec.data("contents")))
      } finally {
        stream.close()
      }
    }).getOrElse(
      println(f"${"Record ID:"}%-20s ${cmd.record_id} not found.")
    )

    ok
  }

  private def do_share(state: State, cmd: Sharing with Command): CLIError \/ Unit = {
    val req = cmd match {
      case AddSharing(content_type, reader) => {
        val user_id = state.config.client_id    // assume writer == user for now
        state.client.authorizeReader(user_id, reader, content_type)
        new PolicyRequest(state.config.client_id,
          state.config.client_id,
          reader,
          Policy.allow(Policy.READ),
          content_type
        )
      }
      case RemoveSharing(content_type, reader) => {
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

  private def do_revoke(state: State, cmd: RevokeSharing): CLIError \/ Unit = {
    val user_id = state.config.client_id // assume writer == user for now
    state.client.revokeSharing(user_id, user_id, cmd.reader)

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
        val meta = new Meta(state.config.client_id,
          cmd.user_id.getOrElse(state.config.client_id),
          cmd.ctype)
        val dataMap = obj.as[Map[String, String]].value.get
        val record = new Record(meta, dataMap)
        val record_id = state.client.writeRecord(record)

        println(record_id)
        ok
      }
    }

  }

  /** Write a file */
  private def do_writefile(state: State, cmd: WriteFile): CLIError \/ Unit = {
    val path = Paths.get(cmd.filename)
    val filename = path.getFileName.toString
    val data = Files.readAllBytes(path)
    val meta = new Meta(state.config.client_id,
      cmd.user_id.getOrElse(state.config.client_id),
      cmd.ctype)

    val fileTypeMap = new MimetypesFileTypeMap()

    val dataMap = Map(
      "filename" -> filename,
      "content-type" -> fileTypeMap.getContentType(filename),
      "contents" -> Base64.encode(data)
    )

    val record = new Record(meta, dataMap)
    val record_id = state.client.writeRecord(record)

    println(record_id)
    ok
  }

  /** Read a CAB from E3DB and print it. */
  private def do_getcab(state: State, cmd: GetCab): CLIError \/ Unit = {
    state.client.getCab(cmd.writer_id, cmd.user_id, cmd.record_type).asScala.map({ cab =>

      println(f"${"CAB Version:"}%-20s ${cab.version}")
      println(f"${"Authorizer ID:"}%-20s ${cab.authorizer.id}")
      println(f"${"Writer ID:"}%-20s ${cab.writer.id}")
      println(f"${"User ID:"}%-20s ${cab.user.id}")

      println(f"\n${"Reader ID"}%-40s ${"Encrypted Access Key"}")
      println("-" * 78)

      cab.pairs.foreach { pair =>
        println(f"${pair.reader_id}%-40s ${pair.eak}")
      }

    }).getOrElse(
      println("CAB not found.")
    )

    ok
  }

  /** Read a client's public key from E3DB and print it. */
  private def do_getkey(state: State, cmd: GetKey): CLIError \/ Unit = {
    state.client.getClientKey(cmd.client_id).asScala.map(_.toJson).map({ keyJson =>
      \/.fromEither(JsonParser.parse(keyJson)).map { obj =>
        println(obj.spaces2)
      }.leftMap(DataError)
    }).getOrElse(DataError(s"Key not found").left)
  }

  /** Process the requested command, synchronously. */
  private def run(opts: Options, client: Client, config: Config): CLIError \/ Unit = {
    val state = State(opts, config, client)

    opts.command match {
      case cmd : Ls => do_ls(state, cmd)
      case cmd : Read => do_read(state, cmd)
      case cmd : ReadFile => do_readfile(state, cmd)
      case cmd : Write => do_write(state, cmd)
      case cmd : WriteFile => do_writefile(state, cmd)
      case cmd : Sharing => do_share(state, cmd)
      case cmd : RevokeSharing => do_revoke(state, cmd)
      case cmd : GetCab => do_getcab(state, cmd)
      case cmd : GetKey => do_getkey(state, cmd)
      case       Info => do_info(state)
      case       Register       => ok
    }
  }

  private def runWithOpts(opts: Options): CLIError \/ Unit = {
    if (opts.command == Register) {
      do_register(opts)
    } else {
      for {
        config <- Config.load(opts.config_file)
        keyManager = ConfigFileKeyManager.get(opts.config_file.getParent.resolve("e3db_key.json").toFile)
        cabManager = new ConfigCabManagerBuilder()
          .setKeyManager(keyManager)
          .setClientId(config.client_id)
          .setConfigDir(new ConfigDir(opts.config_file.getParent.toFile))
          .build()
        client = new HttpE3DBClientBuilder()
          .setClientId(config.client_id)
          .setServiceUri(config.api_url)
          .setApiKeyId(config.api_key_id)
          .setApiSecret(config.api_secret)
          .setKeyManager(keyManager)
          .setCabManager(cabManager)
          .build()
        res <- run(opts, client, config)
      } yield res
    }
  }

  def main(args: Array[String]) {
    (try {
      runWithOpts(OptionParser.parse(args))
    } catch {
      case e: E3DBClientError => ClientError(e.getMessage, e).left
      case e: Exception      => MiscError(e.getMessage, e).left
    }).leftMap {
      case err: ConfigError => {
        println(err.message)
        println("Run `e3db register' to create an account.")
      }

      case err: MiscError => {
        err.exception.printStackTrace()
      }

      case err: ClientError => {
        err.exception.printStackTrace()
      }

      case err => {
        println(err.message)
      }
    }
  }
}
