/*
*      Copyright 2012-2013 Battams, Derek
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
package sagex.epg.schedulesdirect

import org.apache.log4j.Logger
import org.schedulesdirect.api.Airing
import org.schedulesdirect.api.Config
import org.schedulesdirect.api.EpgClient
import org.schedulesdirect.api.NetworkEpgClient
import org.schedulesdirect.api.Program
import org.schedulesdirect.api.Station
import org.schedulesdirect.api.ZipEpgClient

import sage.EPGDBPublic
import sage.EPGImportPlugin
import sagex.SageAPI
import sagex.api.ChannelAPI
import sagex.api.Configuration
import sagex.api.Global
import sagex.epg.schedulesdirect.data.Channel
import sagex.epg.schedulesdirect.data.LineupMap
import sagex.epg.schedulesdirect.data.SageProgram
import sagex.epg.schedulesdirect.io.EpgDownloader
import sagex.epg.schedulesdirect.io.filters.AiringFilter
import sagex.epg.schedulesdirect.io.filters.ProgramFilter
import sagex.epg.schedulesdirect.io.generators.AiringGenerator
import sagex.epg.schedulesdirect.io.generators.ChannelGenerator
import sagex.epg.schedulesdirect.io.generators.ProgramGenerator
import sagex.epg.schedulesdirect.io.lineups.LineupEditor
import sagex.epg.schedulesdirect.plugin.Plugin
import sagex.epg.schedulesdirect.sagetv.helpers.EPGDBPublicAdvancedImpl
import sagex.epg.schedulesdirect.sagetv.helpers.IEPGDBPublicAdvanced
import sagex.remote.rmi.RMISageAPI

import com.google.code.sagetvaddons.license.License
import com.google.code.sagetvaddons.license.LicenseResponse

class EPGImportPluginSchedulesDirect implements EPGImportPlugin {
	static { Class.forName('sagex.epg.schedulesdirect.plugin.Plugin') } // Init the logger only once
	// tmp debug measures, remove or config!!
	static {
		System.setProperty('sdjson.fs.capture', new File('plugins/sdepg/debug').absolutePath)
		System.setProperty('sdjson.capture.encode-errors', '1')
		System.setProperty('sdjson.capture.json-errors', '1')
		System.setProperty('sdjson.capture.http', '1')
	}
	static private final Logger LOG = Logger.getLogger(EPGImportPluginSchedulesDirect)
	static final File EPG_SRC = new File(Plugin.RESOURCE_DIR, 'sdjson.epg')
	static final File LOGOS_ROOT = new File('ChannelLogos')
	
	private IEPGDBPublicAdvanced db
	private Map processedPrograms
	private def uniqueShows
	private def skippedShows
	private def reloadedShows
	private ProgramFilter programFilter
	private AiringFilter airingFilter
	private ProgramGenerator programGenerator
	private AiringGenerator airingGenerator
	private ChannelGenerator chanGenerator
	private boolean licWarnLogged
	private boolean showFilterDisabledLogged
	private boolean airFilterDisabledLogged
	private LicenseResponse licResp
	private boolean programFiltersEnabled
	private boolean airFiltersEnabled
	private boolean programGeneratorsEnabled
	private boolean airGeneratorsEnabled
	private boolean chanGeneratorsEnabled
	private boolean lineupEditorsEnabled
	private boolean installLogosEnabled
	private String providerId
	private String lineupType

	public EPGImportPluginSchedulesDirect() {
		db = null
		processedPrograms = new HashMap<String, Program>()
		programFilter = new ProgramFilter()
		airingFilter = new AiringFilter()
		programGenerator = new ProgramGenerator(new File(Plugin.RESOURCE_DIR, 'program_generators').getAbsolutePath())
		airingGenerator = new AiringGenerator(new File(Plugin.RESOURCE_DIR, 'airing_generators').getAbsolutePath())
		chanGenerator = new ChannelGenerator(new File(Plugin.RESOURCE_DIR, 'channel_generators').getAbsolutePath())
	}

	// Not supporting local markets
	@Override
	public String[][] getLocalMarkets() {
		return new String[0][]
	}

	@Override
	public String[][] getProviders(String zipCode) {
		def sdId = Configuration.GetServerProperty(Plugin.PROP_SD_USER, '')
		def sdPwd = Configuration.GetServerProperty(Plugin.PROP_SD_PWD, '')
		def url = Configuration.GetServerProperty(Plugin.PROP_SDJSON_URL, Config.DEFAULT_BASE_URL)
		licResp = License.isLicensed(Plugin.PLUGIN_ID)		
		def providers = []
		def clnt = null
		try {
			if(EPG_SRC.exists()) {
				try {
					clnt = new ZipEpgClient(EPG_SRC)
				} catch(IOException e) {
					LOG.warn 'Invalid zip file detected; ignoring it!', e
				}
			}
			if(!clnt)
				clnt = new NetworkEpgClient(sdId, sdPwd, EpgDownloader.generateUserAgent(), url, false)
				
			/*
			 *  NOTE: Ignore the zipCode arg; sdjson will just pull headends configured in
			 *  the user's SD account, which is exactly what is needed.
			 */
			clnt.getLineups().each {
				def hash = LineupMap.addId("$it.id:$it.type")
				providers.add([hash.toString(), "$it.name $it.location (${it.type})"])
			}
		} catch(Exception e) {
			LOG.error('Error accessing Schedules Direct', e)
			throw e
		} finally {
			if(clnt)
				try { clnt.close() } catch(IOException e) {}
		}
		if(providers.size() > 1 && !licResp.isLicensed()) {
			LOG.warn 'Unlicensed version of plugin detected; only one lineup will be returned!'
			def minProvider = null
			for(List p : providers)
				if(minProvider == null || p[0].toInteger() < minProvider[0].toInteger())
					minProvider = p
			LOG.warn "Returning lineup ${minProvider[0]}: '${minProvider[1]}'"
			return [minProvider]
		}
		
		LOG.debug "Returning lineups: $providers"
		return providers
	}

	@Override
	public boolean updateGuide(String providerId, EPGDBPublic db) {
		LOG.debug "updateGuide() called for '$providerId'"
		this.providerId = LineupMap.getId(providerId.toLong())
		if(!this.providerId) {
			LOG.error "Provider id is unknown, you need to reconfigure your input sources! [$providerId]"
			return true
		}
		def typeIndex = this.providerId.lastIndexOf(':')
		this.lineupType = this.providerId.substring(typeIndex + 1)
		this.providerId = this.providerId.substring(0, typeIndex)
		LOG.debug "Mapped $providerId to ${this.providerId}; type=$lineupType"
		licResp = License.isLicensed(Plugin.PLUGIN_ID)
		licWarnLogged = false
		showFilterDisabledLogged = false
		airFilterDisabledLogged = false
		programFiltersEnabled = Boolean.parseBoolean(Configuration.GetServerProperty(Plugin.PROP_SHOW_FILTERS, 'true'))
		airFiltersEnabled = Boolean.parseBoolean(Configuration.GetServerProperty(Plugin.PROP_AIR_FILTERS, 'true'))
		airGeneratorsEnabled = Boolean.parseBoolean(Configuration.GetServerProperty(Plugin.PROP_AIR_GENERATORS, 'true'))
		chanGeneratorsEnabled = Boolean.parseBoolean(Configuration.GetServerProperty(Plugin.PROP_CHAN_GENERATORS, 'true'))
		lineupEditorsEnabled = Boolean.parseBoolean(Configuration.GetServerProperty(Plugin.PROP_LINEUP_EDITORS, 'true'))
		programGeneratorsEnabled = Boolean.parseBoolean(Configuration.GetServerProperty(Plugin.PROP_SHOW_GENERATORS, 'true'))
		installLogosEnabled = Boolean.parseBoolean(Configuration.GetServerProperty(Plugin.PROP_INST_LOGOS, 'false'))
		uniqueShows = 0
		skippedShows = 0
		reloadedShows = 0
		processedPrograms.clear()
		this.db = db ? new EPGDBPublicAdvancedImpl((sage.z)db) : null
		if(installLogosEnabled && this.db) {
			LOGOS_ROOT.deleteDir()
			LOGOS_ROOT.mkdir()
			LOG.info 'Deleted existing channel logos in SageTV; installing logos from Schedules Direct.'
		}
		processProgramGenerators()
		doUpdate()
		airingFilter.resetLogger()
		programFilter.resetLogger()
		LOG.debug "Processed ${uniqueShows + skippedShows + reloadedShows} show(s); UNIQUE: ${uniqueShows}; RELOADED: ${reloadedShows}; SKIPPED: ${skippedShows}"
		processAiringGenerators()
		processedPrograms.clear()
		return true
	}

	protected void installLogo(Station s) {
		if(s.logo) {
			def ext = s.logo.url.toString()
			ext = ext.substring(ext.lastIndexOf('.') + 1)
			try {
				s.logo.writeImageToFile(new File(LOGOS_ROOT, "${s.callsign}.$ext"))
			} catch(Exception e) {
				LOG.error('IOError', e)
			}
		}
	}
	
	protected void processAiringGenerators() {
		if(airGeneratorsEnabled) {
			if(licResp.isLicensed()) {
				airingGenerator.generate().each {
					if(db && !db.addAiringPublic2(it.programId, it.stationId, it.startTime, it.durationMillis, getPartsByte(it.partNumber, it.totalParts), getMiscInt(it), it.tvRating != Airing.TvRating.NONE ? it?.tvRating.toString() : null))
						LOG.error "Failed to insert airing details for:\n$it\n"
					else if(db && it.broadcastLanguage != null && it.broadcastLanguage.toLowerCase() != 'english') {
						// TODO Must be able to load a Program from wiz.bin, reset the lang desc then reinsert it into wiz.bin
						// Going to have to be able to create a SageProgram from wiz.bin??  A lot of hassle for something unlikely to be used so leaving it for now
						// If someone needs this they'll see the logged warning below and raise an issue ticket; then I'll figure it out
						LOG.warn "Generated airing has a non-English broadcast languge, but it's ignored because it cannot be set from Airing generators at this time! [$it.id :: $it.broadcastLanguage]"
					}
				}
			} else
				LOG.warn "Not processing airing generators: ${licResp.getMessage()}"
		} else
			LOG.warn 'Not processing airing generators because they are disabled!'
	}
	
	protected void processProgramGenerators() {
		if(programGeneratorsEnabled) {
			if(licResp.isLicensed()) {
				programGenerator.generate().each {
					if(db && !db.addShowPublic2(it.title, it.episodeTitle, it.description, 0L,
			it.categories, it?.credits?.names as String[], it?.credits?.roles as byte[], it.rating,
			it.advisories, it.year > 0 ? it.year.toString() : null, null, null, it.id, null,
			it.originalAirDate ? it.originalAirDate.time : 0L, it.seasonNumber, it.episodeNumber,
			it.forceUnique))
						LOG.error "Failed to insert show details for '$it.externalId'"
				}
			} else
				LOG.warn "Not processing show generators: ${licResp.getMessage()}"
		} else
			LOG.warn 'Not processing show generators because they are disabled!'
	}
	
	protected SageProgram addProgram(Program prog, boolean forceLoad = false) {
		if(processedPrograms.containsKey(prog.id)) {
			if(!forceLoad) {
				++skippedShows
				return processedPrograms.get(prog.id)
			}
			++reloadedShows
		} else
			++uniqueShows
		SageProgram sProg = new SageProgram(prog)
		
		if(LOG.isTraceEnabled()) {
			StringBuilder showDump = new StringBuilder('\n==== NEW SageProgram (pre-filter) ====\n')
			showDump.append sProg.toString()
			showDump.append '==== END SageProgram ====\n\n'
			LOG.trace(showDump.toString())
		}
		
		if(programFiltersEnabled) {
			if(licResp.isLicensed()) {
				programFilter.filter(sProg)
				if(LOG.isTraceEnabled()) {
					StringBuilder showDump = new StringBuilder('\n==== NEW SageProgram (post-filter) ====\n')
					showDump.append sProg.toString()
					showDump.append '==== END SageProgram ====\n\n'
					LOG.trace(showDump.toString())
				}
			} else if(!licWarnLogged){
				LOG.warn "Program and airing filters disabled because license validation failed: ${licResp.getMessage()}"
				licWarnLogged = true
			}
		} else if(!showFilterDisabledLogged) {
			LOG.warn 'Program filters are disabled in plugin settings!'
			showFilterDisabledLogged = true
		}
		
		def catList = [sProg.category]
		sProg.subcategories.each { catList << it }
		if(db && !db.addShowPublic2(sProg.title, sProg.episodeTitle, sProg.description, 0L,
			catList as String[], sProg.sageCredits.getNames() as String[],
			sProg.sageCredits.getRoles() as byte[], sProg.mpaaRating != Program.MPAARating.NONE ? sProg.mpaaRating.toString() : null, sProg.advisories,
			sProg.year > 0 ? sProg.year.toString() : null, null, getBonusDetails(sProg), sProg.id, getLanguage(sProg),
			sProg.originalAirDate ? sProg.originalAirDate.getTime() : 0L, sProg.seasonNum as short,
			sProg.episodeNum as short, sProg.forceUnique)) {
			LOG.error "Failed to add show to database for '$show.externalId'"
			return null
		}
		processedPrograms.put prog.id, sProg
		return sProg
	}

	protected String getLanguage(SageProgram sProg) {
		def lang = sProg.descriptionLanguage
		if(lang == null || lang.length() == 0 || lang.toLowerCase() == 'english')
			return null
		else
			return lang
	}
	
	protected String[] getBonusDetails(SageProgram sProg) {
		if(sProg.starRating)
			return [sProg.starRating] as String[]
		return null
	}
	
	protected boolean doUpdate() {
		def rc = true
		def start = System.currentTimeMillis()
		def sdId = Configuration.GetServerProperty(Plugin.PROP_SD_USER, '')
		def sdPwd = Configuration.GetServerProperty(Plugin.PROP_SD_PWD, '')
		try {
			if(db)
				new EpgDownloader(sdId, sdPwd).download()
		} catch(IOException e) {
			LOG.error 'Download of EPG data failed!', e
			Global.DebugLog('EPG update failed: Download of data from sd4j failed!  See plugin logs for details.')
			return false
		}
		LOG.info "Performed EPG download in ${System.currentTimeMillis() - start}ms"

		start = System.currentTimeMillis()
		addChannels()
		LOG.info "Performed channel processing in ${System.currentTimeMillis() - start}ms"
		
		start = System.currentTimeMillis()
		setLineupMap()
		setPhysicalLineupMap()
		LOG.info "Performed lineup map configuration in ${System.currentTimeMillis() - start}ms"
		
		start = System.currentTimeMillis()
		def clnt = null
		try {
			clnt = new ZipEpgClient(EPG_SRC)
			clnt.getLineupByUriPath(EpgClient.getUriPathForLineupId(providerId)).stations.findAll {
				def chan = ChannelAPI.GetChannelForStationID(it.id.toInteger())
				return chan != null && ChannelAPI.IsChannelViewable(chan)
			}.each {
				it.airings.each { addProgram(it.program); addAiring(it) }
				if(installLogosEnabled) { installLogo(it) }
			}
			LOG.info "Performed EPG data load in ${System.currentTimeMillis() - start}ms"
		} catch(Exception e) {
			LOG.error 'Error processing EPG zip data!', e
		} finally {
			if(clnt)
				try { clnt.close() } catch(IOException e) {}
		}
		return true // Log the error, tell the core we're good for 24 hours; user will eventually investigate when their EPG data goes missing
	}
	
	private boolean addAiring(Airing air) {
		if(LOG.isTraceEnabled()) {
			def msg = new StringBuilder('\n==== NEW AIRING (pre-filter) ====\n')
			msg.append air.toString()
			msg.append('==== END AIRING ====\n\n')
			LOG.trace msg.toString()
		}

		if(airFiltersEnabled) {
			if(licResp.isLicensed()) {
				airingFilter.filter(air)
				if(LOG.isTraceEnabled()) {
					def msg = new StringBuilder('\n==== NEW AIRING (post-filter) ====\n')
					msg.append air.toString()
					msg.append('==== END AIRING ====\n\n')
					LOG.trace msg.toString()
				}
			} else if(!licWarnLogged) {
				LOG.warn "Program and airing filters are ignored because license validation failed: ${licResp.getMessage()}"
				licWarnLogged = true
			}
		} else if(!airFilterDisabledLogged) {
			LOG.warn 'Airing filters are disabled in the plugin settings!'
			airFilterDisabledLogged = true
		}
		def lang = air.broadcastLanguage
		if(lang && lang.toLowerCase() != 'english' && !air.program.descriptionLanguage) {
			def prog = air.program
			if(LOG.isTraceEnabled())
				LOG.trace "Overriding program language for $prog.id; '$prog.descriptionLanguage' => '$lang'"
			prog.descriptionLanguage = lang
			addProgram(prog, true)
		}
		return db && !db.addAiringPublic2(air.id, air.station.id.toInteger(), air.gmtStart.time, 1000L * air.duration, getPartsByte(air.partNum, air.totalParts), getMiscInt(air), air.tvRating != Airing.TvRating.NONE ? air.tvRating.toString() : null)
	}

	private boolean isShowFirstRun(Program p, Airing a) {
		return a.gmtStart.time <= (p.originalAirDate ? p.originalAirDate.getTime() : 0L) + (21L * 86400000L)
	}
	
	// Will only work with an SD Airing or SageAiring object
	private int getMiscInt(def air) {
		int i = 0
		if(air.closedCaptioned)
			i |= IEPGDBPublicAdvanced.CC_MASK
		if(air.stereo)
			i |= IEPGDBPublicAdvanced.STEREO_MASK
		if(air.hdtv)
			i |= IEPGDBPublicAdvanced.HDTV_MASK
		if(air.premiereStatus != null && air.premiereStatus != Airing.PremiereStatus.NONE) {
			switch(air.premiereStatus) {
				case Airing.PremiereStatus.SERIES_PREMIERE: i |= IEPGDBPublicAdvanced.SERIES_PREMIERE_MASK; break
				case Airing.PremiereStatus.SEASON_PREMIERE: i |= IEPGDBPublicAdvanced.SEASON_PREMIERE_MASK; break
				default: i |= IEPGDBPublicAdvanced.PREMIERE_MASK
			}
		}
		switch(air.finaleStatus) {
			case Airing.FinaleStatus.SERIES_FINALE: i |= IEPGDBPublicAdvanced.SERIES_FINALE_MASK; break
			case Airing.FinaleStatus.SEASON_FINALE: i |= IEPGDBPublicAdvanced.SEASON_FINALE_MASK; break
		}
		if(air.liveStatus == Airing.LiveStatus.LIVE)
			i |= IEPGDBPublicAdvanced.LIVE_MASK
		return i
	}
	
	private byte getPartsByte(int partNum, int totalParts) {
		if(!partNum || !totalParts) return 0
		byte b = partNum
		b = b << 4
		b |= totalParts
		return b
	}
	
	private boolean setLineupMap() {
		def lineupDir = ZipEpgClient.scrubFileName("$providerId:$lineupType")
		def root = new File("${Plugin.RESOURCE_DIR}/lineup_editors/$lineupDir")
		if(!root.exists() && !root.mkdirs()) {
			LOG.error "Unable to create lineup editor directory! [$root.absolutePath]"
			return true
		}
		def map = [:]
		def clnt = null
		try {
			clnt = new ZipEpgClient(EPG_SRC)
			map = clnt.getLineupByUriPath(EpgClient.getUriPathForLineupId(providerId)).stationMap
		} catch(Exception e) {
			LOG.error 'sdjson error!', e
		} finally {
			if(clnt) try { clnt.close() } catch(IOException e) {}
		}
		if(map.keySet().size() == 0) {
			LOG.error 'Cannot process an empty lineup map!'
			return false
		}
		def sageMap = [:]
		map.each { k, v -> sageMap[k.toInteger()] = v as String[] }
		if(lineupEditorsEnabled) {
			if(licResp.isLicensed()) {
				def editor = new LineupEditor(root)
				editor.edit(sageMap).each { int k, v -> sageMap[k] = v as String[] }
			} else
				LOG.warn "Lineup editors ignored: ${licResp.getMessage()}"
		} else
			LOG.warn 'Lineup editors ignored because they are disabled!'
		if(db) db.setLineup LineupMap.addId("$providerId:$lineupType"), sageMap
		if(LOG.isDebugEnabled()) LOG.debug "Lineup '$providerId:$lineupType' created: $sageMap"
		return true
	}

	private boolean setPhysicalLineupMap() {
		def lineupDir = ZipEpgClient.scrubFileName("$providerId:$lineupType")
		def root = new File("${Plugin.RESOURCE_DIR}/lineup_editors/$lineupDir")
		def map = [:]
		def clnt = null
		try {
			clnt = new ZipEpgClient(EPG_SRC)
			def lineup = clnt.getLineupByUriPath(EpgClient.getUriPathForLineupId(providerId))
			if(!lineup.hasPhysicalMapping()) {
				LOG.debug "There is no physical mapping to process for this lineup! [$providerId:$lineupType]"
				return true
			}
			LOG.debug "Processing physical mapping for lineup $providerId:$lineupType"
			map = lineup.physicalStationMap
		} catch(Exception e) {
			LOG.error 'sd4j error!', e
		} finally {
			if(clnt) try { clnt.close() } catch(IOException e) {}
		}
		if(map.keySet().size() == 0) {
			LOG.error 'Cannot process an empty lineup map!'
			return false
		}
		def sageMap = [:]
		map.each { k, v -> sageMap[k.toInteger()] = v as String[] }
		if(lineupEditorsEnabled) {
			if(licResp.isLicensed()) {
				def editor = new LineupEditor(root)
				editor.edit(sageMap).each { int k, v -> sageMap[k] = v as String[] }
			} else
				LOG.warn "Lineup editors ignored: ${licResp.getMessage()}"
		} else
			LOG.warn 'Lineup editors ignored because they are disabled!'
		if(db) db.setPhysicalLineup LineupMap.addId("$providerId:$lineupType"), sageMap
		if(LOG.isDebugEnabled()) LOG.debug "Lineup '$providerId:$lineupType' created: $sageMap"
		return true
	}

	private boolean addChannels() {
		def rc = true
		def chans = []
		def clnt = null
		try {
			clnt = new ZipEpgClient(EPG_SRC)
			[clnt.getLineupByUriPath(EpgClient.getUriPathForLineupId(providerId))].each {
				it.stations.each {
					def chanId = it.id
					def callsign = it.callsign
					def longName = it.name
					def network = it.affiliate
					if(db && !db.addChannelPublic(callsign, longName, network, chanId.toInteger())) {
						rc = false
						LOG.error "Failed to add channel to Sage DB! [$callsign :: $chanId]"
					} else {
						chans.add(new Channel(callsign: callsign, longName: longName, network: network, stationId: chanId.toInteger()))
						if(LOG.isTraceEnabled())
							LOG.trace "Channel created: $chanId :: $callsign ($longName)"
					}
				}
			}
		} catch(Exception e) {
			LOG.error 'sdjson error', e
			rc = false
		} finally {
			if(clnt) try { clnt.close() } catch(IOException e) {}
		}
		if(chanGeneratorsEnabled) {
			if(licResp.isLicensed()) {
				chanGenerator.generate(chans).each {
					if(db && !db.addChannelPublic(it.callsign, it.longName, it.network, it.stationId)) {
						rc = false
						LOG.error "Failed to add channel to Sage DB! ($it)"
					}
					if(LOG.isDebugEnabled())
						LOG.debug "Channel created from user generator: $it"
				}
			} else
				LOG.warn "Not processing channel generators: ${licResp.getMessage()}"
		} else
			LOG.warn 'Not processing channel generators because they are disabled!'
		return rc
	}
	
	static void main(String[] args) {
		SageAPI.setProvider(new RMISageAPI(args[0]))
		new EPGImportPluginSchedulesDirect().updateGuide('508278033', null)
	}
}
