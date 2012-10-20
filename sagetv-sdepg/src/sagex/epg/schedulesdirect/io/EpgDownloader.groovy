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
package sagex.epg.schedulesdirect.io

import org.apache.commons.io.FileUtils
import org.apache.log4j.Logger

import sagex.api.Configuration
import sagex.epg.schedulesdirect.plugin.Plugin

class EpgDownloader {
	static private final Logger LOG = Logger.getLogger(EpgDownloader)
	
	private def id
	private def pwd
	private def headend
	
	EpgDownloader(def headend, def id, def pwd) {
		this.id = id
		this.pwd = pwd
		this.headend = headend
	}
	
	void download() throws IOException {
		def targetDir = new File("${Plugin.RESOURCE_DIR}/data/$headend")
		if(!targetDir.exists())
			targetDir.mkdirs()
		def targetFile = new File(targetDir, 'epg.zip')
		if(targetFile.exists() && FileUtils.isFileNewer(targetFile, new Date().time - (3600000L * Configuration.GetServerProperty(Plugin.PROP_EPG_TTL, '23').toLong()))) {
			LOG.info "Using headend data for '$headend' from local cache!"
			return
		}
		def cmd = [new File("${System.getProperty('java.home')}/bin/java").absolutePath, '-jar', new File("${Plugin.RESOURCE_DIR}/tools/sd4j.jar").absolutePath]
		cmd << '-u' << id << '-p' << pwd << '-e' << headend << '-o' << targetDir.absolutePath << '-f' << targetFile.name
		LOG.info cmd
		def p = cmd.execute()
		def stdout = new StringBuilder()
		def stderr = new StringBuilder()
		p.consumeProcessOutput(stdout, stderr)
		if(p.waitFor()) {
			LOG.error("sd4j download failed! [rc=${p.exitValue()}]")
			LOG.error("stdout:\n$stdout")
			LOG.error("stderr:\n$stderr")
			throw new IOException("sd4j download failed! [rc=${p.exitValue()}]")
		} else if(LOG.isDebugEnabled()) {
			LOG.debug("stdout:\n$stdout")
			LOG.debug("stderr:\n$stderr")
		}
	}
}
