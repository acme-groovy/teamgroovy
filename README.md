# teamgroovy
teamcity groovy build step

forked from https://github.com/melix/teamcity-groovy-buildstep

### download 

get in [releases the teamgroovy.zip](https://github.com/dlukyanov/teamgroovy/releases)

### install

In TeamCity 2017 just upload the `teamgroovy.zip` from the `Administration | Plugins List` menu.

For more info see: https://confluence.jetbrains.com/display/TCD10/Installing+Additional+Plugins

### main changes from original:

* support `println` groovy command
* pre-initialized `ant` variable with `groovy.util.AntBuilder`
* project refactored for maven
* compiled and tested for TeamCity 2017+

### screenshot

![screenshot](https://raw.githubusercontent.com/dlukyanov/teamgroovy/master/assets/teamgroovy.png)

### variables

* `ant` : the groovy ant builder (type `groovy.util.AntBuilder`)
* `env` : environment variables (type `Map`)
* `system` : system properties (type `Map`)
* `params` : all build parameters (type `Map`)
* `config` : configuration parameters: <code>config['teamcity.build.workingDir']</code> will return current work dir (type `Map`)
* `agent`: the build agent (type `jetbrains.buildServer.agent.AgentRunningBuild`)
* `context` : the build execution context (type: `jetbrains.buildServer.agent.BuildRunnerContext`)
* `log`: build runner logger (type: `jetbrains.buildServer.agent.BuildProgressLogger`)

### logging

For messages to appear into the build log, you can use `println` groovy function or `log` variable:

```groovy
def name = 'world'

println "hello $name ! from println"
log.message "hello $name ! from logger"
log.warning "hello $name ! from logger in warning mode"
log.error   "hello $name ! from logger in error mode"

ant.echo( message: "hello $name ! from ant" )
```

### ant examples

```groovy
ant.zip(destfile: 'sources.zip', basedir: 'src')
```

for different ant tasks check this: http://ant.apache.org/manual-1.9.x/tasklist.html

more AntBuilder examples see here: http://docs.groovy-lang.org/latest/html/documentation/ant-builder.html

### write all env variables to json

```groovy
new File("${myOutputDir}/env.json").withWriter("UTF-8"){ w->
  new groovy.json.JsonBuilder( env ).writeTo( w )
}
```

### building from source

use java7+ and maven 3+

command to build:
```
mvn clean package
```


