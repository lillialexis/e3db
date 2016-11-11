//
// options.scala --- Command line option parsing.
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

package com.tozny.e3db.cli

import java.nio.file.{InvalidPathException, Path, Paths}
import java.util.UUID

import scalaz._
import scalaz.std.AllInstances._
import scalaz.syntax.apply._
import net.bmjames.opts._

/**
 * Command-line options to the E3DB utility.
 *
 * This class contains global options that are applicable to all
 * commands. Each command contains options that are local to itself.
 */
case class Options (
  verbose: Boolean,
  private val config: Path,
  private val profile: Option[String],
  command: Command
) {

  val config_dir = profile.map(config.resolve(_)).getOrElse(config)

}

sealed trait Command

case class Read(dest: Option[String], raw: Boolean, record_id: UUID) extends Command
case class ReadFile(dest: Option[String], record_id: UUID) extends Command
case class Write(user_id: Option[UUID], ctype: String, data: NonEmptyList[String]) extends Command
case class WriteFile(user_id: Option[UUID], ctype: String, filename: String) extends Command
case class Delete(record_id: UUID) extends Command
case class Ls(limit: Int, offset: Int) extends Command
case object Register extends Command
case object Info extends Command
case object Feedback extends Command

// TODO: These should arguably be subcommands like "e3db cab get"...
case class GetCab(writer_id: UUID, user_id: UUID, record_type: String) extends Command
case class GetKey(client_id: UUID) extends Command

sealed trait Sharing

case class AddSharing(ctype: String, reader: UUID) extends Command with Sharing
case class RemoveSharing(ctype: String, reader: UUID) extends Command with Sharing
case class RevokeSharing(reader: UUID) extends Command

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
  private def parseDir = eitherReader { s =>
    try {
      \/-(Paths.get(s).toAbsolutePath)
    } catch {
      case _: InvalidPathException =>
        -\/(s"Invalid path: ${s}")
    }
  }

  private val validProfile = """^([\p{IsAlphabetic}|\p{Digit}|\.\-_ ]+)$""".r
  private def parseProfile = eitherReader { s =>
    s.trim() match {
      case validProfile(p) => \/-(p)
      case p => -\/(s"Invalid profile name. Profiles can only contain letters, numbers, dashes, periods, underscores and spaces: ${p}")
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
      strArgument(metavar("FILENAME"), help("Path to file to write to E3DB")))(WriteFile)

  private val deleteOpts: Parser[Command] =
    (argument[UUID](parseUUID, metavar("RECORD_ID"), help("Record ID to delete"))).map(Delete.apply)

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
    (argument(readStr, metavar("CONTENT_TYPE"), help("Type of content to share.")) |@|
      argument(parseUUID, metavar("READER_ID"), help("ID of reader.")))(AddSharing)

  private val denyOpts: Parser[Command] =
    (argument(readStr, metavar("CONTENT_TYPE"), help("Type of content to stop sharing."))  |@|
      argument(parseUUID, metavar("READER_ID"), help("ID of reader.")))(RemoveSharing)

  private val revokeOpts: Parser[Command] =
    (argument(parseUUID, metavar("READER_ID"), help("ID of reader."))).map(RevokeSharing.apply)

  // Default location of the E3DB CLI config file.
  private val defaultConfigDir = Paths.get(System.getProperty("user.home"), ".tozny")

  private val parseOpts: Parser[Options] =
    // Global options:
    (switch(short('v'), long("verbose"), help("Enable verbose output")) |@|

     option[Path](parseDir, long("config"), short('c'),
                  help("CLI configuration directory"),
                  value(defaultConfigDir), metavar("DIRECTORY"),
                  showDefaultWith(_.toString)) |@|

      optional(option[String](parseProfile, long("profile"), short('p'),
        help("Use a the E3DB client ID & key stored under the given profile in the configuration directory."),
        metavar("PROFILE"))) |@|

     // Subcommands:
     subparser[Command](
       command("info",      info(pure(Info),               progDesc("Display configuration info."))),
       command("read",      info(readOpts <*> helper,      progDesc("Read data from E3DB."))),
       command("readfile",  info(readFileOpts <*> helper,  progDesc("Read a file from E3DB."))),
       command("write",     info(writeOpts <*> helper,     progDesc("Write data to E3DB."))),
       command("writefile", info(writeFileOpts <*> helper, progDesc("Write a file to E3DB."))),
       command("delete",    info(deleteOpts <*> helper,    progDesc("Delete a record from E3DB."))),
       command("ls",        info(lsOpts <*> helper,        progDesc("List my records in E3DB."))),
       command("register",  info(pure(Register),           progDesc("Register an account with E3DB."))),
       command("getcab",    info(getCabOpts <*> helper,    progDesc("Retrieve a CAB from E3DB."))),
       command("getkey",    info(getKeyOpts <*> helper,    progDesc("Retrieve a client's public key."))),
       command("feedback",  info(pure(Feedback),           progDesc("Provide E3DB feedback to Tozny."))),
       command("share",     info(shareOpts <*> helper,     progDesc("Start sharing records with the given reader.")))/*,
       not yet working
       command("deny",     info(denyOpts <*> helper,  progDesc("Stop sharing records with the given user.")))*/,
       command("revoke",    info(revokeOpts <*> helper,      progDesc("Revoke all sharing with the given reader.")))
    ))(Options)

  private val opts = info(parseOpts <*> helper, progDesc("Tozny E3DB CLI"))
  private val optPrefs = prefs(showHelpOnError)

  /**
   * Parse command-line options and return an {@code Options} object.
   *
   * This will exit the program with an error message and help text
   * if an error occurs.
   */
  def parse(args: Array[String]): Options =
    customExecParser(args.toList, "e3db", optPrefs, opts)
}
