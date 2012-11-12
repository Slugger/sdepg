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

import org.apache.log4j.Logger
import org.schedulesdirect.api.NetworkEpgClient
import org.schedulesdirect.api.ZipEpgClient

import sagex.api.Global
import sagex.api.PluginAPI
import sagex.epg.schedulesdirect.EPGImportPluginSchedulesDirect
import sagex.epg.schedulesdirect.plugin.Plugin

class EpgDownloader {
	static private final Logger LOG = Logger.getLogger(EpgDownloader)
	static String generateUserAgent() {
		def plugin = PluginAPI.GetInstalledPlugins().find { PluginAPI.GetPluginIdentifier(it) == 'sdepg' }
		def ver = plugin ? PluginAPI.GetPluginVersion(plugin) : 'unknown'
		return "sagetv-sdepg/$ver (${System.getProperty('os.name')} ${System.getProperty('os.arch')} ${System.getProperty('os.version')})"
	}
	
	private def id
	private def pwd
	private def newDataReady
	
	EpgDownloader(def id, def pwd) {
		this.id = id
		this.pwd = pwd
		def src = EPGImportPluginSchedulesDirect.EPG_SRC
		try {
			if(!src.canRead())
				newDataReady = true
			else {
				def clnt = new NetworkEpgClient(id, pwd)
				def nextUpdate = Global.GetTimeUntilNextEPGDownload()
				if(nextUpdate <= 0)
					nextUpdate = 86400000L
				def last = System.currentTimeMillis() - (86400000L - nextUpdate)
				newDataReady = clnt.userStatus.isNewDataAvailable(new Date(last))
				LOG.debug "Checking SD server, is new data available? $newDataReady"
			}
		} catch(Exception e) {
			LOG.error('Error checking SD status', e)
			newDataReady = !src.canRead()
		}
	}
	
	void download() throws IOException {
		def targetDir = EPGImportPluginSchedulesDirect.EPG_SRC.parentFile
		if(!targetDir.exists())
			targetDir.mkdirs()
		if(!newDataReady) {
			LOG.info 'Using EPG data from local cache!'
			return
		}
		def targetFile = EPGImportPluginSchedulesDirect.EPG_SRC
		def plugin = PluginAPI.GetInstalledPlugins().find { PluginAPI.GetPluginIdentifier(it) == 'sdepg' }
		def cmd = [new File("${System.getProperty('java.home')}/bin/java").absolutePath, '-jar', new File("${Plugin.RESOURCE_DIR}/tools/sd4j.jar").absolutePath]
		cmd << '-c' << 'grab' << '-u' << id << '-p' << pwd << '-o' << targetFile.absolutePath << '-a' << generateUserAgent() << '-t' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SD4J_THREADS)
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
