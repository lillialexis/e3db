//
// error.scala --- CLI internal error handling.
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

package com.tozny.pds.cli

sealed trait CLIError {
  val message: String
}

case class ConfigError(message: String) extends CLIError
case class ClientError(message: String) extends CLIError
case class DataError(message: String) extends CLIError
