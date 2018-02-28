<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<link rel="stylesheet" href="${teamcityPluginResourcesPath}codemirror.css">

<forms:workingDirectory/>

<tr>
  <th>
      <label for="scriptClasspath">Script classpath:</label>
  </th>
  <td>
      <props:textProperty name="scriptClasspath" className="longField" expandable="true"/>
      <span class="smallNote">Additional semicolon separated classpath. Could contain folders with external groovy classes, so you can use them in this build step.</span>
  </td>
</tr>
<tr id="script.content.container">
  <th>
    <label for="scriptBody">Groovy script: <l:star/></label>
  </th>
  <td>
    <span class="smallNote">A Groovy script which will be executed on the build agent.</span>
      <div class="postRel">
        <textarea id="scriptBody" name="prop:scriptBody">${propertiesBean.properties['scriptBody']}</textarea>
      </div>
    <span class="error" id="error_script.content"></span>
  </td>
</tr>
<tr class="advancedSetting">
  <th>
    <label>variables available in script:</label>
  </th>
  <td>
  	<ul>
		<li><code style="font-weight: bold;">ant</code> ( <a href="http://docs.groovy-lang.org/latest/html/documentation/ant-builder.html">AntBuilder</a> ) -
			<span class="inline">AntBuilder with output redirected to teamcity build logger</span></li>
		<li><code style="font-weight: bold;">system</code> ( <a href="https://docs.oracle.com/javase/7/docs/api/java/util/Map.html">Map</a> ) -
			<span class="inline">system properties</span></li>
		<li><code style="font-weight: bold;">env</code> ( <a href="https://docs.oracle.com/javase/7/docs/api/java/util/Map.html">Map</a> ) -
			<span class="inline">environment variables</span></li>
		<li><code style="font-weight: bold;">config</code> ( <a href="https://docs.oracle.com/javase/7/docs/api/java/util/Map.html">Map</a> ) -
			<span class="inline">configuration parameters: <code>config['teamcity.build.workingDir']</code> will return current work dir</span></li>
		<li><code style="font-weight: bold;">params</code> ( <a href="https://docs.oracle.com/javase/7/docs/api/java/util/Map.html">Map</a> ) -
			<span class="inline">all parameters</span></li>
		<li><code style="font-weight: bold;">log</code> ( <a href="http://javadoc.jetbrains.net/teamcity/openapi/current/jetbrains/buildServer/agent/BuildProgressLogger.html">BuildProgressLogger</a> ) -
			<span class="inline">build step logger</span></li>
		<li><code style="font-weight: bold;">agent</code> ( <a href="http://javadoc.jetbrains.net/teamcity/openapi/current/jetbrains/buildServer/agent/AgentRunningBuild.html">AgentRunningBuild</a> ) -
			<span class="inline">Represents running build on the agent side</span></li>
		<li><code style="font-weight: bold;">context</code> ( <a href="http://javadoc.jetbrains.net/teamcity/openapi/current/jetbrains/buildServer/agent/BuildRunnerContext.html">BuildRunnerContext</a> ) -
			<span class="inline">Represents current build runner.</span></li>
		<li><code style="font-weight: bold;">out</code> ( <a href="https://docs.oracle.com/javase/7/docs/api/java/io/PrintStream.html">PrintStream</a> ) -
			<span class="inline">The printstream that redirects output to teamcity build logger. Allows usage of <code style="font-weight: bold;">println</code> groovy method.</span></li>
	</ul>
  </td>
</tr>

<script>
	$j.getScript("${teamcityPluginResourcesPath}codemirror.js")
		.then(function () {
			return $j.getScript("${teamcityPluginResourcesPath}groovy.js");
		})
		.then(function () {
			var textarea = $("scriptBody");
			var myCodeMirror = CodeMirror.fromTextArea(textarea, {
				lineNumbers: true,
				matchBrackets: true,
				mode: "groovy"
			});
			myCodeMirror.on("change", function (cm) {
				textarea.value = cm.getValue();
			});
		});
</script>
