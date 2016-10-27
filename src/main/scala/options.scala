//
// options.scala --- Command line option parsing.
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

package com.tozny.pds.cli

import java.nio.file.{InvalidPathException,Path,Paths}
import java.util.UUID

import scalaz._
import scalaz.std.AllInstances._
import scalaz.syntax.apply._

import net.bmjames.opts._

/**
 * Command-line options to the PDS utility.
 *
 * This class contains global options that are applicable to all
 * commands. Each command contains options that are local to itself.
 */
case class Options (
  verbose: Boolean,
  config_file: Path,
  command: Command
)

sealed trait Command

case class Read(dest: Option[String], raw: Boolean, record_id: UUID) extends Command
case class ReadFile(dest: Option[String], record_id: UUID) extends Command
case class Write(user_id: Option[UUID], ctype: String, data: NonEmptyList[String]) extends Command
case class WriteFile(user_id: Option[UUID], ctype: String, filename: String) extends Command
case class Ls(limit: Int, offset: Int) extends Command
case object Register extends Command
case object Info extends Command

// TODO: These should arguably be subcommands like "pds cab get"...
case class GetCab(writer_id: UUID, user_id: UUID, record_type: String) extends Command
case class GetKey(client_id: UUID) extends Command

sealed trait Sharing

case class AddSharing(reader: UUID, ctype: String) extends Command with Sharing
case class RemoveSharing(reader: UUID, ctype: String) extends Command with Sharing

object OptionParser {
  /** Argument parser for UUID parameters. */
  private def parseUUID = eitherReader { s =>
    try {
      \/-(UUID.fromString(s))
    } catch {
      case _: IllegalArgumentException =>
        -\/(s"Invalid UUID: ${s}")
    }
  }

  /** Argument parser for a java.nio.file.Path. */
  private def parsePath = eitherReader { s =>
    try {
      \/-(Paths.get(s))
    } catch {
      case _: InvalidPathException =>
        -\/(s"Invalid path: ${s}")
    }
  }

  // Options for the `read` command:
  private val readOpts: Parser[Command] =
    (optional(strOption(long("dest"), help("File to write data to"))) |@|
     switch(short('r'), long("raw"), help("Output raw (encrypted) data")) |@|
     argument(parseUUID, metavar("RECORD_ID"), help("Record ID to read")))(Read)

  private val readFileOpts: Parser[Command] =
    (optional(strOption(long("dest"), help("Override the filename to write data to"))) |@|
      argument(parseUUID, metavar("RECORD_ID"), help("Record ID to read")))(ReadFile)

  // Options for the `write` command:
  private val writeOpts: Parser[Command] =
    (optional(option[UUID](parseUUID, long("user"), help("Owning user of record"))) |@|
     strArgument(metavar("TYPE"), help("Record type")) |@|
     some(strArgument(metavar("DATA"), help("JSON data to store (@FILENAME to read from file)"))))(Write)

  private val writeFileOpts: Parser[Command] =
    (optional(option[UUID](parseUUID, long("user"), help("Owning user of record"))) |@|
      strArgument(metavar("TYPE"), help("Record type")) |@|
      strArgument(metavar("FILENAME"), help("Path to file to write to PDS")))(WriteFile)

  // Options for the `ls` command:
  private val lsOpts: Parser[Command] =
    (option[Int](readInt, long("limit"),
            showDefault, help("Number of results per page"), value(50)) |@|
     option[Int](readInt, long("offset"),
            showDefault, help("Index of first result returned"), value(0)))(Ls)

  // Options for the `getcab` command:
  private val getCabOpts: Parser[Command] =
    (argument[UUID](parseUUID, metavar("WRITER_ID"), help("ID of writer")) |@|
     argument[UUID](parseUUID, metavar("USER_ID"),   help("ID of user")) |@|
      strArgument(metavar("RECORD_TYPE"), help("Type of records.")))(GetCab)

 // Options for the `getkey` command:
 private val getKeyOpts: Parser[Command] =
   (argument[UUID](parseUUID, metavar("CLIENT_ID"), help("ID of client"))).map(GetKey.apply)

  private val shareOpts: Parser[Command] =
    (argument(parseUUID, metavar("READER_ID"), help("ID of reader.")) |@|
      argument(readStr, metavar("CONTENT_TYPE"), help("Type of content to share.")))(AddSharing)

  private val denyOpts: Parser[Command] =
    (argument(parseUUID, metavar("READER_ID"), help("ID of reader.")) |@|
     argument(readStr, metavar("CONTENT_TYPE"), help("Type of content to stop sharing.")))(RemoveSharing)

  // Default location of the PDS CLI config file.
  private val defaultConfigFile =
    Paths.get(System.getProperty("user.home"), ".tozny", "pds.json")

  private val parseOpts: Parser[Options] =
    // Global options:
    (switch(short('v'), long("verbose"), help("Enable verbose output")) |@|
     option[Path](parsePath, long("config"), short('c'),
                  help("CLI configuration file"),
                  value(defaultConfigFile), metavar("FILENAME"),
                  showDefaultWith(_.toString)) |@|

     // Subcommands:
     subparser[Command](
       command("info",      info(pure(Info),               progDesc("Display configuration info"))),
       command("read",      info(readOpts <*> helper,      progDesc("Read data from the PDS"))),
       command("readfile",  info(readFileOpts <*> helper,  progDesc("Read a file from the PDS"))),
       command("write",     info(writeOpts <*> helper,     progDesc("Write data to the PDS"))),
       command("writefile", info(writeFileOpts <*> helper, progDesc("Write a file to the PDS"))),
       command("ls",        info(lsOpts <*> helper,        progDesc("List my records in the PDS"))),
       command("register",  info(pure(Register),           progDesc("Register an account with the PDS"))),
       command("getcab",    info(getCabOpts <*> helper,    progDesc("Retrieve a CAB from the PDS"))),
       command("getkey",    info(getKeyOpts <*> helper,    progDesc("Retrieve a client's public key"))),
       command("share",     info(shareOpts <*> helper,     progDesc("Start sharing records with the given user.")))/*,
       not yet working
       command("deny",     info(denyOpts <*> helper,  progDesc("Stop sharing records with the given user.")))*/
    ))(Options)

  private val opts = info(parseOpts <*> helper, progDesc("Tozny Personal Data Service CLI"))
  private val optPrefs = prefs(showHelpOnError)

  /**
   * Parse command-line options and return an {@code Options} object.
   *
   * This will exit the program with an error message and help text
   * if an error occurs.
   */
  def parse(args: Array[String]): Options =
    customExecParser(args.toList, "pds", optPrefs, opts)
}
