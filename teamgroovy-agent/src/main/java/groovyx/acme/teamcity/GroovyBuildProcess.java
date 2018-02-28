/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovyx.acme.teamcity;

import groovy.util.logging.Log4j;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.apache.log4j.Logger;

import groovy.lang.GroovyClassLoader;
import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.AntBuilder;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.runtime.StackTraceUtils;

//import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.DefaultLogger;

import java.util.List;
import java.io.PrintStream;
import java.io.File;
import java.util.Map;

import java.util.concurrent.*;

public class GroovyBuildProcess implements BuildProcess, Callable<BuildFinishedStatus> {
	private static final Logger log = Logger.getLogger(GroovyBuildProcess.class);

	private Future<BuildFinishedStatus> buildStatus;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final AgentRunningBuild agent;
	private final BuildRunnerContext context;

	public GroovyBuildProcess(final AgentRunningBuild agent, final BuildRunnerContext context) {
		this.agent = agent;
		this.context = context;
	}

	@Override
	public void start() throws RunBuildException {
		try {
			buildStatus = executor.submit(this);
			log.info("Groovy script started");
		} catch (final RejectedExecutionException e) {
			log.error("Groovy script failed to start", e);
			throw new RunBuildException(e);
		}
	}

	public boolean isInterrupted() {
		return buildStatus.isCancelled() && isFinished();
	}

	public boolean isFinished() {
		return buildStatus.isDone();
	}

	public void interrupt() {
		log.info("Interrupting Groovy script");
		buildStatus.cancel(true);
	}

	@Override
	public BuildFinishedStatus waitFor() throws RunBuildException {
		try {
			final BuildFinishedStatus status = buildStatus.get();
			log.info("Build process was finished");
			return status;
		} catch (final InterruptedException e) {
			throw new RunBuildException(e);
		} catch (final ExecutionException e) {
			throw new RunBuildException(e);
		} catch (final CancellationException e) {
			log.error("Build process was interrupted: ", e);
			return BuildFinishedStatus.INTERRUPTED;
		} finally {
			executor.shutdown();
		}
	}

	private AntBuilder getAntBuilder(PrintStream out, PrintStream err){
		AntBuilder ant = new AntBuilder();
		//List<BuildListener> listeners = ant.getProject().getBuildListeners();
		//replace stdout and stderr in default ant logger to redirect output to teamcity
		for(BuildListener listener : ant.getProject().getBuildListeners()){
			if(listener instanceof DefaultLogger){
				DefaultLogger defLogger = (DefaultLogger)listener;
				defLogger.setOutputPrintStream(out);
				defLogger.setErrorPrintStream(err);
			}
		}
		return ant;
	}


	public BuildFinishedStatus call() {
		try {
			PrintStream out = new PrintStream( new LogStream(agent.getBuildLogger(), LogStream.LEVEL_INFO), true, "UTF-8" );
			PrintStream err = new PrintStream( new LogStream(agent.getBuildLogger(), LogStream.LEVEL_ERR),  true, "UTF-8" );
			/*
			Map<String,Object> teamcity = (Map)Configs.propsToTreeMap( context.getConfigParameters(), "^teamcity\\\\..*" ).get("teamcity");
			if(teamcity==null)teamcity=new ConcurrentSkipListMap();
		    */
			Binding binding = new Binding();
			binding.setProperty("system", context.getBuildParameters().getSystemProperties());
			binding.setProperty("env", context.getBuildParameters().getEnvironmentVariables());
			binding.setProperty("params", context.getBuildParameters().getAllParameters());
			binding.setProperty("agent", agent);
			binding.setProperty("config", context.getConfigParameters());
			binding.setProperty("log", agent.getBuildLogger());
			binding.setProperty("context", context);
			binding.setProperty("out", out);
			binding.setProperty("ant", getAntBuilder( out, err ));

			GroovyShell shell = new GroovyShell(binding);

			//add classpath to groovy shell
			String classpath = context.getRunnerParameters().get("scriptClasspath");
			if(classpath!=null && classpath.length()>0){
				GroovyClassLoader cl = shell.getClassLoader();
				for (String cp : classpath.split(";")) {
					cp = cp.trim();
					if( ! new File(cp).exists() )agent.getBuildLogger().warning("path not found: "+cp);
					cl.addClasspath(cp);
				}
			}

			Script script = shell.parse(context.getRunnerParameters().get("scriptBody"));
			Object result = script.run();
			agent.getBuildLogger().message("Groovy script: SUCCESS");
		} catch (Throwable e) {
			//agent.getBuildLogger().error(e.toString());
			agent.getBuildLogger().exception( StackTraceUtils.deepSanitize(e) );
			return BuildFinishedStatus.FINISHED_FAILED;
		}

		return BuildFinishedStatus.FINISHED_SUCCESS;
	}


}
