<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="admin" tagdir="/WEB-INF/tags/admin" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<link rel="stylesheet" href="${teamcityPluginResourcesPath}codemirror.css">

<forms:workingDirectory/>

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
<tr>
  <th>
    <label>Variables available in groovy script:</label>
  </th>
  <td>
  	<table border="0">
  		<tr>
  		<th>name</th><th>type</th><th>comment</th>
  		</tr>
  		<tr>
			<td>system</td>
			<td><a href="https://docs.oracle.com/javase/7/docs/api/java/util/Map.html">java.util.Map</a></td>
			<td>system properties</td>
  		</tr>
  		<tr>
			<td>env</td>
			<td><a href="https://docs.oracle.com/javase/7/docs/api/java/util/Map.html">java.util.Map</a></td>
			<td>environment variables</td>
  		</tr>
  		<tr>
			<td>configParams</td>
			<td><a href="https://docs.oracle.com/javase/7/docs/api/java/util/Map.html">java.util.Map</a></td>
			<td>configuration parameters</td>
  		</tr>
  		<tr>
			<td>params</td>
			<td><a href="https://docs.oracle.com/javase/7/docs/api/java/util/Map.html">java.util.Map</a></td>
			<td>all parameters</td>
  		</tr>
  		<tr>
			<td>log</td>
			<td><a href="http://javadoc.jetbrains.net/teamcity/openapi/current/jetbrains/buildServer/agent/BuildProgressLogger.html">jetbrains.buildServer.agent.BuildProgressLogger</a></td>
			<td>build step logger</td>
  		</tr>
  		<tr>
			<td>agent</td>
			<td><a href="http://javadoc.jetbrains.net/teamcity/openapi/current/jetbrains/buildServer/agent/AgentRunningBuild.html">jetbrains.buildServer.agent.AgentRunningBuild</a></td>
			<td>Represents running build on the agent side</td>
  		</tr>
  		<tr>
			<td>context</td>
			<td><a href="http://javadoc.jetbrains.net/teamcity/openapi/current/jetbrains/buildServer/agent/BuildRunnerContext.html">jetbrains.buildServer.agent.BuildRunnerContext</a></td>
			<td>Represents current build runner.</td>
  		</tr>
  		<tr>
			<td>out</td>
			<td><a href="https://docs.oracle.com/javase/7/docs/api/java/io/PrintStream.html">java.io.PrintStream</a></td>
			<td>The printstream that redirects output to teamcity build logger. Allows usage of <code>println</code> groovy method.</td>
  		</tr>
  		<tr>
			<td>ant</td>
			<td><a href="http://docs.groovy-lang.org/latest/html/documentation/ant-builder.html">groovy.util.AntBuilder</a></td>
			<td>AntBuilder with output redirected to teamcity build logger</td>
  		</tr>
  	</table>
  </td>
</tr>

<script>
    $j.getScript("${teamcityPluginResourcesPath}codemirror.js")
    .done(function () {
    	console.log("${teamcityPluginResourcesPath}groovy.js");
        return $j.getScript("${teamcityPluginResourcesPath}groovy.js");
    })
    .done(function () {
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
