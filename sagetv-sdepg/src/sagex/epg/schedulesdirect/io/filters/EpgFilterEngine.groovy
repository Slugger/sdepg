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
package sagex.epg.schedulesdirect.io.filters

import org.apache.log4j.Appender
import org.apache.log4j.FileAppender
import org.apache.log4j.Layout
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.SimpleLayout

abstract class EpgFilterEngine implements Runnable {
	static private final Logger LOG = Logger.getLogger(EpgFilterEngine)
	static final String OBJ_VAR_NAME = '_src_'
	static final String LOG_VAR_NAME = '_log_'
	static final String SCRIPT_EXTENSION = '.groovy'
	static final String APPENDER_NAME = '_sdepg'
	
	private String root
	private GroovyScriptEngine engine
	private String[] files
	private String logName

	EpgFilterEngine(def root, def type) {
		this.root = root
		engine = new GroovyScriptEngine([this.root] as String[])
		files = null
		logName = "sagex.epg.schedulesdirect.logging.filters.$type"
	}

	void resetLogger() {
		Logger log = Logger.getLogger(logName)
		log.getAllAppenders().toList().each {
			it.close()
			log.removeAppender(it)
		}
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
	
	protected void filter(String scriptName, def target) {
		File absPath = new File(root + File.separator + scriptName).getAbsoluteFile()
		Binding binding = new Binding()
		binding.setVariable OBJ_VAR_NAME, target
		binding.setVariable LOG_VAR_NAME, getLogger(absPath.name.substring(0, absPath.name.lastIndexOf('.')))
		try {
			engine.run(scriptName, binding)
		} catch(ScriptException e) {
			LOG.error "Parsing of '$absPath' failed!", e
		} catch(RuntimeException e) {
			LOG.error "Runtime error with '$absPath'!", e
		}
	}

	synchronized void filter(def target) {
		if(files == null) {
			files = new File(root).list(new FilenameFilter() {
				boolean accept(File f, String fName) {
					return fName.toLowerCase().endsWith(EpgFilterEngine.SCRIPT_EXTENSION)
				}
			})
			if(files != null) {
				Arrays.sort files
				new Thread(this).start()
			}
		}
		if(files)
			files.each { filter(it, target) }
	}
	
	void run() {
		sleep 300000
		synchronized(this) { files = null }
		LOG.debug 'Cleared script cache from filter engine!'
	}
}
