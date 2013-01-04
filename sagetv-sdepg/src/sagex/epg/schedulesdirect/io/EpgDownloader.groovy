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

import org.apache.commons.io.IOUtils
import org.apache.log4j.Logger

import sagex.api.ChannelAPI
import sagex.api.Configuration
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
	
	EpgDownloader(def id, def pwd) {
		this.id = id
		this.pwd = pwd
	}
		
	void download() throws IOException {
		def targetDir = EPGImportPluginSchedulesDirect.EPG_SRC.parentFile
		if(!targetDir.exists())
			targetDir.mkdirs()
		def targetFile = EPGImportPluginSchedulesDirect.EPG_SRC
		def plugin = PluginAPI.GetInstalledPlugins().find { PluginAPI.GetPluginIdentifier(it) == 'sdepg' }
		def cmd = [new File("${System.getProperty('java.home')}/bin/java").absolutePath, "-Xmx${PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_GRABBER_HEAP)}m", '-jar', new File("${Plugin.RESOURCE_DIR}/tools/sdjson.jar").absolutePath]
		cmd << '-c' << 'grab' << '-u' << id << '-p' << pwd << '-o' << targetFile.absolutePath << '-a' << generateUserAgent() << '-t' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SDJSON_THREADS)
		cmd << '-b' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SDJSON_URL)
		cmd << '-pc' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SDJSON_PROG_CHUNK)
		cmd << '-sc' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_SDJSON_SCHED_CHUNK)
		cmd << '-l' << PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_GRABBER_LOG_LVL)
		def ignoreFile = new File(targetDir, 'ignore.txt')
		ignoreFile.delete()
		def ignoreList = ChannelAPI.GetAllChannels().findAll { !ChannelAPI.IsChannelViewable(it) }
		if(ignoreList.size() > 0) {
			ignoreFile.withWriterAppend('UTF-8') { f ->
				ignoreList.each { f.append(Integer.toString(ChannelAPI.GetStationID(it)) + IOUtils.LINE_SEPARATOR)}
			}
			cmd << '-g' << ignoreFile.absolutePath
		}
		def cachePurged = false
		if(System.currentTimeMillis() - (PluginAPI.GetPluginConfigValue(plugin, Plugin.PROP_CACHE_TTL).toLong() * 86400000L) > Configuration.GetServerProperty(Plugin.PROP_LAST_CACHE_PURGE, '0').toLong()) {
			cmd << '-x'
			cachePurged = true
		}
		LOG.info cmd.toString().replace(pwd, '*****')
		def p = cmd.execute()
		def stdout = new StringBuilder()
		def stderr = new StringBuilder()
		p.consumeProcessOutput(stdout, stderr)
		if(p.waitFor()) {
			LOG.error("sdjson download failed! [rc=${p.exitValue()}]")
			LOG.error("stdout:\n$stdout")
			LOG.error("stderr:\n$stderr")
			throw new IOException(" download failed! [rc=${p.exitValue()}]")
		} else {
			if(cachePurged)
				Configuration.SetServerProperty(Plugin.PROP_LAST_CACHE_PURGE, System.currentTimeMillis().toString())
			if(LOG.isDebugEnabled()) {
				LOG.debug("stdout:\n$stdout")
				LOG.debug("stderr:\n$stderr")
			}
		}
		ignoreFile.delete()
	}
}
