//
// error.scala --- CLI internal error handling.
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

package com.tozny.e3db.cli

import scalaz._
import scalaz.syntax.either._

import com.tozny.e3db.client.errors._

sealed trait CLIError {
  val message: String
}

case class ConfigError(message: String) extends CLIError
case class ClientError(message: String, exception: Throwable) extends CLIError
case class RegisterError(message: String, exception: Throwable) extends CLIError
case class DataError(message: String) extends CLIError
case class ServiceError(message: String) extends CLIError
case class MiscError(message: String, exception: Throwable) extends CLIError

/**
 * Lift a Scala expression into \/[CLIError, A], catching
 * exceptions and returning error objects as necessary.
 */
object CLIError {
  def handle[A](f: => A): \/[CLIError, A] =
    \/.fromTryCatchNonFatal(f).leftMap {
      case e: E3DBClientError => ClientError(e.getMessage, e)
      case e => MiscError(e.getMessage, e)
    }
}
