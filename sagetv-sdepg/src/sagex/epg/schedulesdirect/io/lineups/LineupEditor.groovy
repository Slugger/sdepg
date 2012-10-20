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
package sagex.epg.schedulesdirect.io.lineups

import org.apache.log4j.Appender
import org.apache.log4j.FileAppender
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.SimpleLayout

class LineupEditor {
	static private final Logger LOG = Logger.getLogger(LineupEditor)
	static final String SCRIPT_NAME = 'channels.groovy'
	static final String APPENDER_NAME = '_sdepg'

	private File root
	private GroovyScriptEngine engine
	private String logName

	LineupEditor(def root) {
		this.root = root
		engine = new GroovyScriptEngine([this.root] as String[])
		logName = "sagex.epg.schedulesdirect.logging.lineups"
	}

	protected Logger getLogger() {
		Logger log = Logger.getLogger(logName)
		log.setAdditivity(false)
		log.setLevel(Level.DEBUG)
		String uniqueName = "${APPENDER_NAME}_${root.absolutePath.hashCode()}"
		log.getAllAppenders().toList().each { it.threshold = Level.OFF }
		Appender app = log.getAppender(uniqueName)
		if(!app) {
			app = new FileAppender(new SimpleLayout(), new File(root, 'channels.log').absolutePath, false)
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

	Map edit(Map<Integer, String[]> lineup) {
		def script = new File(root, SCRIPT_NAME)
		if(!script.exists())
			return [:]
		LOG.debug "Lineup editor processing has started for '$root'!"	
		def start = System.currentTimeMillis()
		Binding binding = new Binding()
		binding.setVariable '_lineup_', lineup
		binding.setVariable('_log_', getLogger())
		def result = null
		try {
			result = engine.run(SCRIPT_NAME, binding)
		} catch(ScriptException e) {
			LOG.error "Parsing of '${SCRIPT_NAME}' failed!", e
		} catch(RuntimeException e) {
			LOG.error "Runtime error with '${SCRIPT_NAME}'!", e
		}
		if(result == null || !(result instanceof Map)) {
			LOG.error "Script '$script' did not return a channel map; result ignored!"
			return [:]
		}
		LOG.info "Processed lineup editor for '$root' in ${System.currentTimeMillis() - start}ms"
		resetLogger()
		return result
	}
}
