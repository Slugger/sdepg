/*
*      Copyright 2011 Battams, Derek
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
package sagex.epg.schedulesdirect.sagetv.helpers

import java.util.Map

import sage.z

/*
 * An implementation of the interface
 */
class EPGDBPublicAdvancedImpl implements IEPGDBPublicAdvanced {

	private def db

	// Pass in an instance of sage.z; you'll receive such an instance as the arg in the EPG plugin call
	EPGDBPublicAdvancedImpl(z db) {
		this.db = db
	}

	/*
	 * Call this to add an Airing to the database. An Airing is time-channel-show correlation.
	 * extID - refers to the GUID of a Show previously added with addShowPublic
	 * stationID - referes to the stationID GUID of a Channel previously added with addChannelPublic
	 * startTime - the time this airing starts, a long from java.util.Date
	 * duration - the length of this airing in milliseconds
	 * partNumber - if it is a multipart show this is the part number, otherwise this should be 0
	 * totalParts - for multipart TV shows, this is the total number of parts otherwise this should be zero
	 * parentalRating - the parental rating for the show, should be a localized value from "TVY", "TVY7", "TVG", "TVPG", "TV14", "TVM" or the empty string
	 * hdtv - true if it's an HDTV airing, false otherwise
	 * stereo - true if it's a stereo recording, false otherwise
	 * closedCaptioning - true if the airing has closed captioning, false otherwise
	 * sap - true if the Airing has a Secondary Audio Program (SAP), false otherwise
	 * subtitled - true if the Airing is subtitled, false otherwise
	 * premierFinale - should be the empty string or a localized value from the list "Premier", "Channel Premier", "Season Premier", "Series Premier", "Season Finale", "Series Finale"
	 *
	 * Returns true if this Airing was successfully updated/added to the database. The database will
	 * automatically ensure that there are no inconsistencies in the Airings, if you add one that
	 * overlaps with the station-time-duration of another. The one(s) that were in the database before
	 * the call will be removed and the new one added. It will also fill in any gaps with "No Data" sections
	 * for you automatically.
	 */
	@Override
	boolean addAiringDetailedPublic(String extId, int stationId, long startTime,
	long duration, int partNumber, int totalParts, String parentalRating, boolean hdtv,
	boolean stereo, boolean closedCaptioning, boolean sap, boolean subtitled,
	String premierFinale) {
		return db.addAiringDetailedPublic(extId, stationId, startTime, duration, partNumber, totalParts, parentalRating, hdtv,
		stereo, closedCaptioning, sap, subtitled, premierFinale)
	}

	/*
	 * Call this to add an Airing to the database. An Airing is time-channel-show correlation.
	 * extID - refers to the GUID of a Show previously added with addShowPublic
	 * stationID - referes to the stationID GUID of a Channel previously added with addChannelPublic
	 * startTime - the time this airing starts, a long from java.util.Date
	 * duration - the length of this airing in milliseconds
	 *
	 * Returns true if this Airing was successfully updated/added to the database. The database will
	 * automatically ensure that there are no inconsistencies in the Airings, if you add one that
	 * overlaps with the station-time-duration of another. The one(s) that were in the database before
	 * the call will be removed and the new one added. It will also fill in any gaps with "No Data" sections
	 * for you automatically.
	 */
	@Override
	boolean addAiringPublic(String extId, int stationId, long startTime, long duration) {
		return db.addAiringPublic(extId, stationId, startTime, duration)
	}

	/*
	* Call this to add a Channel to the database. This will update if the stationID is already
	* used.  name should be the call sign, like KABC. longName can be a full descriptive name like
	* "Channel 4 Los Angeles NBC". network represents the parent network, i.e. ABC, HBO, MTV and is
	* optional. stationID is the GUID referring to this Channel.
	*
	* Returns true if the Channel was successfully updated/added to the database.
	*/
	@Override
	boolean addChannelPublic(String name, String longName, String network, int stationId) {
		return db.addChannelPublic(name, longName, network, stationId)
	}

	// Details not available
	@Override
	boolean addSeriesInfoPublic(int arg0, String arg1, String arg2,
	String arg3, String arg4, String arg5, String arg6, String arg7,
	String arg8, String arg9, String[] arg10, String[] arg11) {
		return db.addSeriesInfoPublic(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11)
	}

	/*
	* Call this to add a Show to the database. If a show with this extID is already present, it will be updated
	* to this information. You can use null or String[0] for any fields you don't want to specify.
	* title - the title of the show (use for reruns)
	* primeTitle - the title of the show (use for first runs)
	* episodeName - the name of the episode
	* desc - a description of this show
	* duration - not used, set to 0
	* category - name of a category for this show
	* subCategory - name of a subCategory for this show
	* people - names of people/actors in this show
	* roles - must be same length as people array, uses the X_ROLE constants in this file to specify what each is
	* rated - rating of a show, i.e. PG, G, R, etc.
	* expandedRatings - additional rating information, i.e. Violence, Nudity, Adult Content
	* year - the year it was produced, for movies
	* parentalRating - not used, set to null
	* bonus - additional information about the show
	* extID - GUID representing this show
	* language - the language the show is in
	* originalAirDate - the original airing date of this show, it's a long value from java.util.Date
	*
	* Returns true if the Show was successfully updated/added to the database.
	*/
	@Override
	boolean addShowPublic(String title, String primeTitle, String episodeName,
	String desc, long duration, String category, String subCategory, String[] people,
	byte[] roles, String rated, String[] expandedRatings, String year,
	String parentalRating, String[] bonus, String extID, String language, long originalAirDate) {
		return db.addShowPublic(title, primeTitle, episodeName, desc, duration, category, subCategory,
			people, roles, rated, expandedRatings, year, parentalRating, bonus, extID, language,
			originalAirDate)
	}

	/*
	* Call this with the current providerID, and a map of stationIDs to channel numbers. The keys in the map
	* should be Integer objects wrapping stationIDs as used in the addChannelPublic method. The values in the map
	* should be String[] that represent the channel numbers for that station. An example is if ESPN w/ stationdID=34
	* is on channel numbers 3 and 94, the map would contain a: Integer(34)->{"3", "94"}
	*/
	@Override
	void setLineup(long providerID, Map lineupMap) {
		db.setLineup(providerID, lineupMap)
	}

	/*
	* Call this with the current providerID, and a map of stationIDs to physical channel numbers. The keys in the map
	* should be Integer objects wrapping stationIDs as used in the addChannelPublic method. The values in the map
	* should be String[] that represent the channel numbers for that station. An example is if ESPN w/ stationdID=34
	* is on channel numbers 3 and 94, the map would contain a: Integer(34)->{"3", "94"}
	* This ONLY needs to be called for channels who's physical number is different from their logical number.
	*/
	@Override
	void setPhysicalLineup(long providerID, Map lineupMap) {
		db.for(providerID, lineupMap)
	}

	/*
	* Call this to add a Show to the database. If a show with this extID is already present, it will be updated
	* to this information. You can use null or String[0] for any fields you don't want to specify.
	* title - the title of the show
	* episodeName - the name of the episode
	* desc - a description of this show
	* duration - not used, set to 0
	* categories - categories for this show
	* people - names of people/actors in this show
	* roles - must be same length as people array, uses the X_ROLE constants in this file to specify what each is
	* rated - rating of a show, i.e. PG, G, R, etc.
	* expandedRatings - additional rating information, i.e. Violence, Nudity, Adult Content
	* year - the year it was produced, for movies
	* parentalRating - not used, set to null
	* bonus - additional information about the show
	* extID - GUID representing this show
	* language - the language the show is in
	* originalAirDate - the original airing date of this show, it's a long value from java.util.Date
	* seasonNum - the season number, 0 if this is undefined
	* episodeNum - the season number, 0 if this is undefined
	* forcedUnique - true if it's known that this Show represents 'unique' content (i.e. all Airings with a Show of this ExternalID will be the EXACT same content)
	*
	* Returns true if the Show was successfully updated/added to the database.
	*/
	@Override
	boolean addShowPublic2(String title, String episodeName,
	String desc, long duration, String[] categories, String[] people,
	byte[] roles, String rated, String[] expandedRatings, String year,
	String parentalRating, String[] bonus, String extID,
	String language, long originalAirDate, short seasonNum,
	short episodeNum, boolean forcedUnique) {
		return db.a(title, episodeName, desc, duration, categories, people, roles, rated,
			expandedRatings, year, parentalRating, bonus, extID, language, originalAirDate,
			seasonNum, episodeNum, forcedUnique)
	}

	/*
	* Call this to add an Airing to the database. An Airing is time-channel-show correlation.
	* extID - refers to the GUID of a Show previously added with addShowPublic
	* stationID - referes to the stationID GUID of a Channel previously added with addChannelPublic
	* startTime - the time this airing starts, a long from java.util.Date
	* duration - the length of this airing in milliseconds
	* partsByte - the highest 4 bits should be the part number of this airing, and the lowest four bits should be the total parts; set this to zero if it's not a multipart Airing
	* misc - integer bitmask of other misc. properties; see above for the constants used here, for 'premiere/finale' info it uses 3 bits for that one value
	* parentalRating - the parental rating for the show, should be a localized value from "TVY", "TVY7", "TVG", "TVPG", "TV14", "TVM" or the empty string
	*
	* Returns true if this Airing was successfully updated/added to the database. The database will
	* automatically ensure that there are no inconsistencies in the Airings, if you add one that
	* overlaps with the station-time-duration of another. The one(s) that were in the database before
	* the call will be removed and the new one added. It will also fill in any gaps with "No Data" sections
	* for you automatically.
	*/
	@Override
	boolean addAiringPublic2(String extID, int stationID,
	long startTime, long duration, byte partsByte, int misc,
	String parentalRating) {
		return db.a(extID, stationID, startTime, duration, partsByte, misc, parentalRating)
	}

}
