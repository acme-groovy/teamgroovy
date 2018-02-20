/*
 * Copyright 2003-2012 the original author or authors.
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

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.util.PropertiesUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

public class GroovyRunType extends RunType {
    private PluginDescriptor descriptor;

    public GroovyRunType(@NotNull final PluginDescriptor descriptor, @NotNull final RunTypeRegistry registry) {
        registry.registerRunType(this);
        this.descriptor = descriptor;
    }

    @NotNull
    @Override
    public String getType() {
        return "teamgroovy";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Groovy script (teamgroovy)";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Allows execution of Groovy scripts as build steps";
    }

    @Nullable
    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return new PropertiesProcessor() {
            @Override
            public Collection<InvalidProperty> process(final Map<String, String> properties) {
                Collection<InvalidProperty> invalid = new LinkedList<InvalidProperty>();
                if (PropertiesUtil.isEmptyOrNull(properties.get("scriptBody"))) {
                    invalid.add(new InvalidProperty("scriptBody", "Script body cannot be empty"));
                }
                return invalid;
            }
        };
    }

    @Nullable
    @Override
    public String getEditRunnerParamsJspFilePath() {
        return descriptor.getPluginResourcesPath("groovyRunParams.jsp");
    }

    @Nullable
    @Override
    public String getViewRunnerParamsJspFilePath() {
        return descriptor.getPluginResourcesPath("viewGroovyParams.jsp");
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return null;
    }
}
