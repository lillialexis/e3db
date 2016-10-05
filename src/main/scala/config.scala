//
// config.scala --- User configuration saving/loading.
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

package com.tozny.pds.cli

import scala.io.StdIn
import scala.collection.JavaConversions._
import java.io.IOException
import java.nio.file.{Files, Path}
import java.nio.file.attribute._
import java.util
import java.util.UUID

import scalaz._
import scalaz.syntax.either._
import argonaut._
import Argonaut._
import org.jose4j.jwk._

/** Configuration file for the PDS CLI. */
case class Config (
  client_id: UUID,
  api_url: String,
  api_key_id: String,
  api_secret: String
)

object Config {
  /** Encode a {@code java.util.UUID} as a JSON string. */
  implicit def UUIDEncodeJson: EncodeJson[UUID] =
    EncodeJson(a => jString(a.toString))

  /** Decode a JSON string to a {@code java.util.UUID}. */
  implicit def UUIDDecodeJson: DecodeJson[UUID] =
    StringDecodeJson.map(a => UUID.fromString(a))

  /** JSON codec for the {@code Config} class. */
  implicit def ConfigJsonCodec: CodecJson[Config] =
    casecodec4(Config.apply, Config.unapply)(
      "client_id", "api_url", "api_key_id", "api_secret"
    )

  /** Load configuration from {@code file}. */
  def load(file: Path): CLIError \/ Config = {
    val s = try {
      new String(Files.readAllBytes(file))
    } catch {
      case e: IOException =>
        return ConfigError(s"Error loading config: ${e.getMessage}").left
    }

    // TODO: Not sure why 'argonaut-scalaz' doesn't add a decode
    // function to restore the 6.1.x behavior here. So for now,
    // we'll manually convert Either[_, _] to '_ \/ _', but
    // I'm not wild about it.
    \/.fromEither(s.decodeEither[Config]).leftMap { err =>
      ConfigError(s"Error loading config: ${err}")
    }
  }

  // Used to test for Windows OS
  private val w = """(?i)(^Win).*""".r

  /** Save configuration to {@code file}. */
  def save(config_file: Path, config: Config): CLIError \/ Unit = try {
    val s = config.asJson.spaces2
    val (fileAttr, dirAttr) = System.getProperty("os.name") match {
      case w(_) => (Seq(), Seq())
      case _ => // Some other OS
        (Seq(PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))),
          Seq(PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))))
    }

    val file = config_file.toAbsolutePath
    Files.createDirectories(file.getParent, dirAttr: _*)

    // We delete the file and create it exclusively to ensure that
    // the permissions are set correctly.
    Files.deleteIfExists(file)
    Files.createFile(file, fileAttr: _*)
    Files.write(file, s.getBytes)
    ().right
  } catch {
    case e: IOException =>
      ConfigError(s"Error saving config: ${e.getMessage}").left
  }
}
