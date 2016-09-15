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
import java.nio.file.{Files,Path}
import java.nio.file.attribute._
import java.util.UUID

import argonaut._, Argonaut._

import org.jose4j.jwk._

/** Configuration file for the PDS CLI. */
case class Config (
  version: Int,
  client_key: Option[PublicJsonWebKey],
  client_id: UUID,
  api_url: String,
  api_key_id: String,
  api_secret: String
)

object Config {
  /** Current version of the configuration format. */
  final val VERSION = 1

  /** Size in bits of the generated RSA key pair. */
  final val KEY_PAIR_BITS = 3072

  /** JSON codec for a jose4j {@code PublicJsonWebKey}. */
  implicit def JWKCodec: CodecJson[PublicJsonWebKey] =
    CodecJson(
      (jwk: PublicJsonWebKey) => {
        val params = jwk.toParams(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE)
        Json.jObject(params.foldLeft(JsonObject.empty) {
          case (json, (name, value)) =>
            json + (name, jString(value.toString))
        })
      },
      c => {
        val params = c.focus.objectOrEmpty.toMap.mapValues(_.stringOrEmpty)
        DecodeResult.ok(PublicJsonWebKey.Factory.newPublicJwk(params))
      }
    )

  /** Encode a {@code java.util.UUID} as a JSON string. */
  implicit def UUIDEncodeJson: EncodeJson[UUID] =
    EncodeJson(a => jString(a.toString))

  /** Decode a JSON string to a {@code java.util.UUID}. */
  implicit def UUIDDecodeJson: DecodeJson[UUID] =
    StringDecodeJson.map(a => UUID.fromString(a))

  /** JSON codec for the {@code Config} class. */
  implicit def ConfigJsonCodec: CodecJson[Config] =
    casecodec6(Config.apply, Config.unapply)(
      "version", "client_key", "client_id",
      "api_url", "api_key_id", "api_secret")

  /** Return a default configuration with a newly generated key. */
  def defaults() = {
    new Config(
      VERSION, None,
      UUID.fromString("00000000-0000-0000-0000-000000000000"),
      "https://api.pds.tozny.com/v1", "", "")
  }

  /** Load configuration from {@code file}. */
  def load(file: Path): Option[Config] = {
    try {
      val s = new String(Files.readAllBytes(file))
      s.decodeOption[Config]
    } catch {
      case _: IOException => None
    }
  }

  /** Save configuration to {@code file}. */
  def save(config_file: Path, config: Config) = {
    val s = config.asJson.spaces2
    val posixFileAttr = PosixFilePermissions.asFileAttribute(
      PosixFilePermissions.fromString("rw-------"))
    val posixDirAttr = PosixFilePermissions.asFileAttribute(
      PosixFilePermissions.fromString("rwx------"))
    // TODO: Add secure Windows permissions.

    val file = config_file.toAbsolutePath
    Files.createDirectories(file.getParent, posixDirAttr)

    // We delete the file and create it exclusively to ensure that
    // the permissions are set correctly.
    Files.deleteIfExists(file)
    Files.createFile(file, posixFileAttr)
    Files.write(file, s.getBytes)
  }

  private def readLineDefault(prompt: String, default: String): String = {
    val value = StdIn.readLine(s"${prompt} [${default}]: ")
    if (value == "") { default } else { value }
  }

  /** Initialize a configuration interactively from a set of defaults. */
  def init(defaults: Config): Config = {
    val api_url = readLineDefault("API URL", defaults.api_url)
    val api_key_id = readLineDefault("API Key ID", defaults.api_key_id)
    val api_secret = readLineDefault("API Secret", defaults.api_secret)
    val client_id = readLineDefault("Client UUID", defaults.client_id.toString)
    val client_key =
      if (defaults.client_key.isEmpty) {
        Some(RsaJwkGenerator.generateJwk(KEY_PAIR_BITS))
      } else {
        defaults.client_key
      }

    Config(VERSION, client_key, UUID.fromString(client_id),
           api_url, api_key_id, api_secret)
  }
}
