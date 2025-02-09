// Copyright 2018 The Alephium Authors
// This file is part of the alephium project.
//
// The library is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// The library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with the library. If not, see <http://www.gnu.org/licenses/>.

package org.alephium.app

import java.net.InetAddress

import com.typesafe.config.{Config, ConfigException}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader

import org.alephium.api.model.ApiKey
import org.alephium.conf._
import org.alephium.util.{Duration, U256}

final case class ApiConfig(
    networkInterface: InetAddress,
    blockflowFetchMaxAge: Duration,
    askTimeout: Duration,
    apiKey: Option[ApiKey],
    gasFeeCap: U256,
    defaultUtxosLimit: Int
)

object ApiConfig {

  implicit private val apiValueReader: ValueReader[ApiKey] =
    ValueReader[String].map { input =>
      ApiKey.from(input) match {
        case Right(apiKey) => apiKey
        case Left(error)   => throw new ConfigException.BadValue("ApiKey", error)
      }
    }

  implicit private val apiConfigValueReader: ValueReader[ApiConfig] =
    valueReader { implicit cfg =>
      ApiConfig(
        as[InetAddress]("networkInterface"),
        as[Duration]("blockflowFetchMaxAge"),
        as[Duration]("askTimeout"),
        as[Option[ApiKey]]("apiKey"),
        as[U256]("gasFeeCap"),
        as[Int]("defaultUtxosLimit")
      )
    }

  def load(config: Config, path: String): ApiConfig = config.as[ApiConfig](path)
  def load(config: Config): ApiConfig               = config.as[ApiConfig]("alephium.api")
}
