/*-
 * <<
 * Moonbox
 * ==
 * Copyright (C) 2016 - 2018 EDP
 * ==
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * >>
 */

package moonbox.common

import java.util.concurrent.ConcurrentHashMap
import moonbox.common.config._
import moonbox.common.util.Utils
import scala.collection.JavaConverters._

class MbConf(loadDefault: Boolean) extends Cloneable with Serializable with MbLogging {

	def this() = this(true)

	private val settings = new ConcurrentHashMap[String, String]()

	@transient private lazy val reader: ConfigReader = {
		val _reader = new ConfigReader(new MbConfigProvider(settings))
		_reader.bindEnv(new ConfigProvider {
			override def get(key: String): Option[String] = {
				Option(Utils.getEnv(key))
			}
		})
		_reader
	}
	if (loadDefault) {
		loadFromSystemProperties()
	}

	private val configFromFile = Utils.getDefaultPropertiesFile() match {
		case Some(file) =>
			Utils.typesafeConfig2Map(Utils.getConfigFromFile(file))
		case None => Map[String, String]()
	}

	mergeConfig(configFromFile)

	private def mergeConfig(config: Map[String, String]): Unit = {
		config.foreach { case (k, v) =>
			settings.putIfAbsent(k, v) // Set in java options has high priority
			sys.props.getOrElseUpdate(k, v)
		}
	}

	def set[T](key: String, value: T): this.type = {
		settings.put(key, value.toString)
		this
	}

	def set[T >: String](pairs: Seq[(String, T)]): this.type = {
		pairs.foreach {
			case (key, value) => set(key, value)
		}
		this
	}

	def getAll: Map[String, String] = settings.asScala.toMap[String, String]

	def getOption(key: String): Option[String] = {
		Option(settings.get(key)).orElse(None)
	}

	def get(key: String): Option[String] = getOption(key)

	def get(key: String, defaultValue: String): String ={
		settings.getOrDefault(key, defaultValue)
	}

	def get(key: String, defaultValue: Int): Int = {
		get(key, defaultValue.toString).toInt
	}

	def get(key: String, defaultValue: Long): Long ={
		get(key, defaultValue.toString).toLong
	}

	def get(key: String, defaultValue: Boolean): Boolean ={
		get(key, defaultValue.toString).toBoolean
	}

	def get(key: String, defaultValue: Double): Double ={
		get(key, defaultValue.toString).toDouble
	}

	def get(key: String, defaultValue: Float): Float ={
		get(key, defaultValue.toString).toFloat
	}

	def get[T](entry: ConfigEntry[T]): T = {
		entry.readFrom(reader)
	}

	private def loadFromSystemProperties(): Unit = {
		Utils.getSystemProperties.foreach { case (k, v) =>
			if (k.startsWith("moonbox.")) {
				set(k, v)
			}
		}
	}

}

