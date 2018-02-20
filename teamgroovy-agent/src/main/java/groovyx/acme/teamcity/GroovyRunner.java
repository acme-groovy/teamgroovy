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

import groovy.transform.CompileStatic;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentBuildRunner;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.AgentExtension;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.jetbrains.annotations.NotNull;

public class GroovyRunner implements AgentExtension, AgentBuildRunner, AgentBuildRunnerInfo {

    @Override
    public BuildProcess createBuildProcess(
            @NotNull final AgentRunningBuild agentRunningBuild,
            @NotNull final BuildRunnerContext buildRunnerContext) throws RunBuildException {
        return new GroovyBuildProcess(agentRunningBuild, buildRunnerContext);
    }

    @Override
    public AgentBuildRunnerInfo getRunnerInfo() {
        return this;
    }

    @Override
    public String getType() {
        return "teamgroovy";
    }

    @Override
    public boolean canRun(@NotNull final BuildAgentConfiguration buildAgentConfiguration) {
        return true;
    }
}
