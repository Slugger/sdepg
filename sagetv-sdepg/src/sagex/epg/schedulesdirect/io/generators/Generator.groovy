/*
*      Copyright 2011-2013 Battams, Derek
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
package sagex.epg.schedulesdirect.io.generators

import org.apache.log4j.Appender
import org.apache.log4j.FileAppender
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.SimpleLayout

abstract class Generator {

	static final String SCRIPT_EXTENSION = '.groovy'
	static final String APPENDER_NAME = '_sdepg'
	static final String LOG_VAR_NAME = '_log_'
	
	protected String root
	protected GroovyScriptEngine engine
	protected String[] files
	protected String logName
	
	Generator(def root, def type) {
		this.root = root
		engine = new GroovyScriptEngine([this.root] as String[])
		files = null
		logName = "sagex.epg.schedulesdirect.logging.generators.$type"
	}
	
	protected Logger getLogger(String name) {
		Logger log = Logger.getLogger(logName)
		log.setAdditivity(false)
		log.setLevel(Level.DEBUG)
		String uniqueName = "${APPENDER_NAME}_$name"
		log.getAllAppenders().toList().each { it.threshold = Level.OFF }
		Appender app = log.getAppender(uniqueName)
		if(!app) {
			app = new FileAppender(new SimpleLayout(), new File(new File(root), "${name}.log").absolutePath, false)
			app.setName(uniqueName)
			app.threshold = Level.DEBUG
			log.addAppender(app)
			log.info "Log created on ${new Date()}"
		} else
			app.threshold = Level.DEBUG
		return log
	}
	
	protected void resetLogger() {
		Logger log = Logger.getLogger(logName)
		log.getAllAppenders().toList().each {
			it.close()
			log.removeAppender(it)
		}
	}
}
