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

import sage.EPGDBPublic

/*
 * This interface was introduced in Sage 7.1.9, but was obfuscated accidentially during the build
 * process.  To add things like the Live flag, etc. for airings requires use of this interface.  I've
 * recreated the interface here and will wrap the obfuscated implementation in an implementation of this
 * interface.  In Sage 7.1.9, you can safely cast the EPGDBPublic argument in an EPG plugin to an
 * instance of this interface and make the calls provided here.
 * 
 * With assistance from Sage support.
 */
interface IEPGDBPublicAdvanced extends EPGDBPublic {
	// The ROLE constants are used in addShowPublic below; these are in addition to the ones
	// provided in EPGDBPublic
	static final byte CONTESTANT_ROLE = 22;
	static final byte CORRESPONDENT_ROLE = 23;
	static final byte TEAM_ROLE = 24;
	static final byte GUEST_VOICE_ROLE = 25;
	static final byte ANCHOR_ROLE = 26;
	static final byte VOICE_ROLE = 27;
	static final byte MUSICAL_GUEST_ROLE = 28;

	// These bit masks are used in addAiringPublic2 below
	static final int CC_MASK = 0x01;
	static final int STEREO_MASK = 0x02;
	static final int HDTV_MASK = 0x04;
	static final int SUBTITLE_MASK = 0x08;
	static final int PREMIERES_MASK = 0x70;
	static final int PREMIERE_MASK = 0x10;
	static final int SEASON_PREMIERE_MASK = 0x20;
	static final int SERIES_PREMIERE_MASK = 0x30;
	static final int CHANNEL_PREMIERE_MASK = 0x40;
	static final int SEASON_FINALE_MASK = 0x50;
	static final int SERIES_FINALE_MASK = 0x60;
	static final int SAP_MASK = 0x80;
	static final int THREED_MASK = 0x100;
	static final int DD51_MASK = 0x200;
	static final int DOLBY_MASK = 0x400;
	static final int LETTERBOX_MASK = 0x800;
	static final int LIVE_MASK = 0x1000;
	static final int NEW_MASK = 0x2000;
	static final int WIDESCREEN_MASK = 0x4000;
	static final int SURROUND_MASK = 0x8000;
	static final int DUBBED_MASK = 0x10000;
	static final int TAPE_MASK = 0x20000;
 
	/*
	* Call this with the current providerID, and a map of stationIDs to physical channel numbers. The keys in the map
	* should be Integer objects wrapping stationIDs as used in the addChannelPublic method. The values in the map
	* should be String[] that represent the channel numbers for that station. An example is if ESPN w/ stationdID=34
	* is on channel numbers 3 and 94, the map would contain a: Integer(34)->{"3", "94"}
	* This ONLY needs to be called for channels who's physical number is different from their logical number.
	*/
   void setPhysicalLineup(long providerID, java.util.Map lineupMap);

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
   public boolean addShowPublic2(String title, String episodeName, String desc, long duration, String[] categories,
	   String[] people, byte[] roles, String rated, String[] expandedRatings,
	   String year, String parentalRating, String[] bonus, String extID, String language, long originalAirDate,
	   short seasonNum, short episodeNum, boolean forcedUnique);

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
   public boolean addAiringPublic2(String extID, int stationID, long startTime, long duration, byte partsByte, int misc, String parentalRating);
}