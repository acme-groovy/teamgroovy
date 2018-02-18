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

import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.lang.GroovyShell;

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

    public BuildFinishedStatus call() {
        Binding binding = new Binding();
        binding.setProperty("system", context.getBuildParameters().getSystemProperties());
        binding.setProperty("env", context.getBuildParameters().getEnvironmentVariables());
        binding.setProperty("params", context.getBuildParameters().getAllParameters());
        binding.setProperty("agent", agent);
        binding.setProperty("configParams", context.getConfigParameters());
        binding.setProperty("log", agent.getBuildLogger());
        binding.setProperty("context", context);

        GroovyShell shell = new GroovyShell(binding);
        Script script = shell.parse(context.getRunnerParameters().get("scriptBody"));
        try {
            Object result = script.run();
            agent.getBuildLogger().message("Script finished with result: $result");
        } catch (Throwable e) {
            agent.getBuildLogger().error(e.toString());
            return BuildFinishedStatus.FINISHED_FAILED;
        }

        return BuildFinishedStatus.FINISHED_SUCCESS;
    }


}
