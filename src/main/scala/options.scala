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
case class Read(dest: Option[String], record_id: UUID) extends Command
case class Write(ctype: String, data: String) extends Command
case class Ls(limit: Int, offset: Int) extends Command
case object Init extends Command

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
     argument(parseUUID, metavar("RECORD_ID"), help("Record ID to read")))(Read)

  // Options for the `write` command:
  private val writeOpts: Parser[Command] =
    (strArgument(metavar("TYPE"), help("Record type")) |@|
     strArgument(metavar("DATA"), help("JSON data to store (@FILENAME to read from file)")))(Write)

  // Options for the `ls` command:
  private val lsOpts: Parser[Command] =
    (option[Int](readInt, long("limit"),
            showDefault, help("Number of results per page"), value(50)) |@|
     option[Int](readInt, long("offset"),
            showDefault, help("Index of first result returned"), value(0)))(Ls)

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
       command("read",  info(readOpts <*> helper,   progDesc("Read data from the PDS"))),
       command("write", info(writeOpts <*> helper,  progDesc("Write data to the PDS"))),
       command("ls",    info(lsOpts <*> helper,     progDesc("List my records in the PDS"))),
       command("init",  info(pure(Init),            progDesc("Initialize preferences"))))
    )(Options)

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
