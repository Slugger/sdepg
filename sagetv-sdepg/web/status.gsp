<%
/*
 *      Copyright 2013 Battams, Derek
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
	def clnt = request.getAttribute('clnt')
	def sysMsgs = clnt.userStatus.systemMessages
	def usrMsgs = clnt.userStatus.userMessages
%>
<h2>Schedules Direct Status</h2>
<ul>
	<li><b>User:</b> $clnt.userStatus.userId</li>
	<li><b>Account Expires:</b> $clnt.userStatus.expires</li>
	<li><b>Last server EPG update:</b> $clnt.userStatus.lastServerRefresh</li>
	<li><b>Server status:</b> $clnt.systemStatus.status as of $clnt.systemStatus.statusDate [msg: $clnt.systemStatus.statusMessage]</li>
	<li><b>Maximum lineups:</b> $clnt.userStatus.maxLineups</li>
	<li><b>Configured lineups:</b> ${clnt.userStatus.lineupInfo.size()}</li>
</ul>
<h2>Schedules Direct Messages</h2>
<% if(!sysMsgs.size() && !usrMsgs.size()) { %>
<h3>No messages.</h3>
<% } else { %>
	<form action="index.gsp" method="post">
		<input type="hidden" name="c" value="rmmsg" />
	<table border="1">
	<tr>
		<th><input type="checkbox" name="_" id="msgs" /></th>
		<th>Type</th>
		<th>Date</th>
		<th>Message</th>
	</tr>
	<% sysMsgs.each { %>
		<tr>
			<td><input class="sdmsg" type="checkbox" name="msgids" value="$it.id" /></td>
			<td>System</td>
			<td>$it.date</td>
			<td>$it.content</td>
		</tr>
	<% } %>
        <% usrMsgs.each { %>
		<tr>
	                <td><input class="sdmsg" type="checkbox" name="msgids" value="$it.id" /></td>
	                <td>User</td>
	                <td>$it.date</td>
	                <td>$it.content</td>
		</tr>
        <% } %>
	</table>
		<input type="submit" name="submit" value="Delete Marked" />
	</form>
<% } %>

<script>
<!--
\$(document).ready(function() {
	\$('#msgs').change(function() {
		var val = \$(this).prop('checked');
		\$('.sdmsg').each(function() {
			\$(this).prop('checked', val);
		});
	});
});
-->
</script>
