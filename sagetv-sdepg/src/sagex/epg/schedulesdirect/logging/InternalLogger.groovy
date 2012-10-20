/*
 *      Copyright 2011-2012 Battams, Derek
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 */
package sagex.epg.schedulesdirect.logging

import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout
import org.apache.log4j.RollingFileAppender

import sagex.epg.schedulesdirect.plugin.Plugin

class InternalLogger {

	static void init(String logLevel) {
		def log = Logger.getLogger('sagex.epg.schedulesdirect')
		def a = log.getAppender('sdepg')
		if(!a) {
			def p = new PatternLayout('%d %-5p [%c{1}]: %m%n')
			a = new RollingFileAppender(p, new File(Plugin.RESOURCE_DIR, 'logs/sdepg.log').getAbsolutePath())
			a.name = 'sdepg'
			a.maxBackupIndex = 2
			a.maxFileSize = '25MB'
			log.addAppender(a)
		}
		log.level = Level.toLevel(logLevel)
		log.additivity = false
	}
	
	private InternalLogger() {}

}
