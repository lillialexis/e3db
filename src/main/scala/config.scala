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

import argonaut._, Argonaut._

import org.jose4j.base64url.Base64Url
import org.jose4j.jwk._
import org.jose4j.keys._
import org.jose4j.lang.ByteUtil

/** Configuration file for the PDS CLI. */
case class Config (
  version: Int,
  client_key: Option[PublicJsonWebKey],
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

  /** JSON codec for the {@code Config} class. */
  implicit def ConfigJsonCodec: CodecJson[Config] =
    casecodec5(Config.apply, Config.unapply)(
      "version", "client_key", "api_url", "api_key_id", "api_secret")

  /** Return a default configuration with a newly generated key. */
  def defaults() = {
    new Config(VERSION, None, "https://api.pds.tozny.com/v1", "", "")
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

  /** Initialize a configuration interactively from a set of defaults. */
  def init(defaults: Config): Config = {
    val api_url = StdIn.readLine(s"API URL [${defaults.api_url}]: ")
    val api_key_id = StdIn.readLine(s"API Key ID [${defaults.api_key_id}]: ")
    val api_secret = StdIn.readLine(s"API Secret [${defaults.api_secret}]: ")

    Config(VERSION,
           if (defaults.client_key.isEmpty) { Some(RsaJwkGenerator.generateJwk(KEY_PAIR_BITS)) } else { defaults.client_key },
           if (api_url == "") { defaults.api_url } else { api_url },
           if (api_key_id == "") { defaults.api_key_id } else { api_key_id },
           if (api_secret == "") { defaults.api_secret } else { api_secret })
  }
}
