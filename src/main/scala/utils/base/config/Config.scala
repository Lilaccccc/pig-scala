package pig
package utils.base.config

import com.typesafe.config.{ConfigFactory, Config as Cfg}

import scala.quoted.*

private[config] trait Config(name: String) {
  val field: String
  def f = s"$name.$field"
}

private[config] lazy val config: Cfg = {
  val active = ConfigFactory.load("application.conf").getString("active")
  ConfigFactory.load(s"application-$active.conf")
}

private[config] inline def get[T](enums: Config): T = ${ getImpl[T]('{ enums.f }) }

private def getImpl[T: Type](field: Expr[String])(using Quotes): Expr[T] = '{ config.getAnyRef($field).asInstanceOf[T] }
