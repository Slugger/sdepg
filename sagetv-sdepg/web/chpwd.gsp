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
%>
<h2>Schedules Direct Password Change</h2>
<form action="index.gsp" method="post">
	<input type="hidden" name="c" value="chpwd" />
	<input type="hidden" name="m" value="chpwd" />
	<div>
		<span>Old Password:</span>
		<span><input type="password" name="p0" /></span>
	</div>
	<div>
		<span>New Password:</span>
		<span><input type="password" name="p1" /></span>
	</div>
	<div>
		<span>New Password (Again):</span>
		<span><input type="password" name="p2" /></span>
	</div>
	<input type="submit" name="submit" value="Change Password" />
</form>