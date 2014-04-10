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
package sagex.epg.schedulesdirect.data

import java.util.regex.Pattern

import org.apache.log4j.Logger
import org.json.JSONObject
import org.schedulesdirect.api.Program

import sage.EPGDBPublic
import sagex.api.Configuration
import sagex.epg.schedulesdirect.plugin.Plugin
import sagex.epg.schedulesdirect.sagetv.helpers.IEPGDBPublicAdvanced

/**
 * @author Derek Battams <derek@battams.ca>
 *
 */
public class SageProgram {
	static private final Logger LOG = Logger.getLogger(SageProgram)
	static private final String TEAM_REGEX_STR = '[A-Z][\\w\\s-&\'\\.\\(\\)\\d]+';
	static private final String EVENT_REGEX_STR = "(${TEAM_REGEX_STR}) (?:at|vs\\.{0,1}|\u00E0|contre|c\\.) (${TEAM_REGEX_STR})";
	static private final Pattern TEAM_REGEX = Pattern.compile(EVENT_REGEX_STR)

	static private final Set UNHANDLED_ROLES = Collections.synchronizedSet(new HashSet())
	
	private Program __src
	private SageCreditList sageCredits
	private boolean forceUnique
	private int seasonNum
	private int episodeNum
	
	/**
	 * 
	 */
	public SageProgram(Program src) {
		this.__src = src
		sageCredits = new SageCreditList()
		def category = src.genres ? src.genres[0] : ''
		if((src.id.startsWith('SP') || category.toLowerCase() == 'sports event') && src.episodeTitle =~ TEAM_REGEX) {
			def m = src.episodeTitle =~ TEAM_REGEX
			sageCredits.add(getCredit('TEAM', m[0][1]))
			sageCredits.add(getCredit('TEAM', m[0][2]))
		}
		forceUnique = false
		src.credits.each { sageCredits.add(getCredit(it.role.toString(), it.name)) }
		checkForSENumbers()
	}
	
	protected void checkForSENumbers() {
		def seSrc = Configuration.GetServerProperty(Plugin.PROP_SE_SRC, '') 
		def allData = __src.metadata.findAll {
			def src = it.keySet().toArray()[0]
			if(it[src] instanceof JSONObject)
				it[src].has('season') && it[src].has('episode') && it[src]['season'].toInteger() > 0 && it[src]['episode'].toInteger() > 0
			else {
				LOG.warn "Metadata for $id is invalid!"
				return false
			}
		}
		def data = null
		if(allData.size() > 0) {
			switch(seSrc) {
				case Plugin.OPT_SE_SRC_TVRAGE_ONLY:
				case Plugin.OPT_SE_SRC_TVRAGE_PREF:
					data = allData.find { it.keySet().toArray()[0] == 'tvrage' }
					if(!data && seSrc == Plugin.OPT_SE_SRC_TVRAGE_PREF) data = allData[0]
					break
				case Plugin.OPT_SE_SRC_TRIBUNE_ONLY:	
				case Plugin.OPT_SE_SRC_TRIBUNE_PREF:
					data = allData.find { it.keySet().toArray()[0] == 'Tribune' }
					if(!data && seSrc == Plugin.OPT_SE_SRC_TRIBUNE_PREF) data = allData[0]
					break
				case Plugin.OPT_SE_SRC_TVDB_ONLY:
				case Plugin.OPT_SE_SRC_TVDB_PREF:
					data = allData.find { it.keySet().toArray()[0] == 'tvdb' }
					if(!data && seSrc == Plugin.OPT_SE_SRC_TVDB_PREF) data = allData[0]
					break
			}
		}
		if(data) {
			def key = data.keySet().toArray()[0]
			def s = data[key]['season']?.toInteger()
			def e = data[key]['episode']?.toInteger()
			if(s > 0 && e > 0) {
				seasonNum = s
				episodeNum = e
			}
		}
	}
	
	@Override
	public def getProperty(String name) {
		def found = false
		def val = null
		if(!name.startsWith('__')) {
			this.getClass().declaredFields.each {
				if(name == it.name) {
					it.setAccessible(true)
					val = it.get(this)
					found = true
				}
			}
		}
		return !found ? __src."$name" : val
	}
	
	@Override
	public void setProperty(String name, Object val) {
		def found = false
		if(!name.startsWith('__')) {
			this.getClass().declaredFields.each {
				if(name == it.name) {
					it.setAccessible(true)
					it.set(this, val)
					found = true
				}
			}
		}
		if(!found)
			__src."$name" = val
	}

	protected SageCredit getCredit(def type, def name) {
		def role
		switch(type.toLowerCase()) {
			case 'actor': role = EPGDBPublic.ACTOR_ROLE; break
			case 'presenter': role = EPGDBPublic.HOST_ROLE; break
			case 'director': role = EPGDBPublic.DIRECTOR_ROLE; break
			case 'guest': role = EPGDBPublic.GUEST_STAR_ROLE; break
			case 'writer': role = EPGDBPublic.WRITER_ROLE; break
			case 'producer': role = EPGDBPublic.PRODUCER_ROLE; break
			case 'executive_producer': role = EPGDBPublic.EXECUTIVE_PRODUCER_ROLE; break
			case 'narrator': role = EPGDBPublic.NARRATOR_ROLE; break
			case 'guest_star': role = EPGDBPublic.GUEST_STAR_ROLE; break
			case 'host': role = EPGDBPublic.HOST_ROLE; break
			case 'judge': role = EPGDBPublic.JUDGE_ROLE; break
			case 'contestant': role = IEPGDBPublicAdvanced.CONTESTANT_ROLE; break
			case 'anchor': role = IEPGDBPublicAdvanced.ANCHOR_ROLE; break
			case 'musical_guest': role = IEPGDBPublicAdvanced.MUSICAL_GUEST_ROLE; break
			case 'correspondent': role = IEPGDBPublicAdvanced.CORRESPONDENT_ROLE; break
			case 'team': role = IEPGDBPublicAdvanced.TEAM_ROLE; break
			default:
				role = EPGDBPublic.ACTOR_ROLE
				name = "$name (${type.capitalize()})"
				if(!UNHANDLED_ROLES.contains(type)) {
					LOG.warn "Unhandled credit type encountered: $type"
					UNHANDLED_ROLES << type
				}
		}
		return new SageCredit(name: name, role: role)
	}
}
