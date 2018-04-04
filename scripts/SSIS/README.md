# Misrosoft SSIS builder/deployer groovy class

## goal 

ability to build and deploy ssis (ispac) projects using groovy script.

## why groovy?

class dedicated to be used from teamcity from `groovy script (teamgroovy)` build step.

there are problems with building/deploying ispac using MSBuild 

also ssis projects are changed a lot by microsoft, so using groovy that you can easily modify without compilation is a good solution (maybe:)

## assumptions

for example you have the following folder structure with SSIS project:
```
./SSIS.groovy
./StageSSIS.json               the project descriptor file (used only from command line)
./StageSSIS/                   your ssis project folder that contains StageSSIS.dtproj and other project files
```

## running from Groovy Script (teamgroovy)

specify in `Script classpath` the folder where `SSIS.groovy` located (checked out)

and the following code will perform build and deploy

```groovy
def ctx=[
	"build" :[
		"dtproj" : "./StageSSIS/StageSSIS.dtproj",
		"config" : "Dev1-SQL"
	],
	"deploy":[
		"folder":"Stage",
		"ssisdb": [
			"driver"  :"net.sourceforge.jtds.jdbc.Driver",
			"url"     : "jdbc:jtds:sqlserver://dev1-sql:1433/SSISDB;useNTLMv2=true;domain=MYDOM",
			"user"    : "teamcity",
			"password": "xyz"
		]
	]
]
SSIS.buildSSIS(ctx)
SSIS.deploySSIS(ctx)
```

## running from command line

**prerequisites**

installed `groovy 2.4+` and `java 1.7+`

**command line**

build and deploy ssis project with parameters from json descriptor
```
groovy SSIS.groovy all ./StageSSIS.json
```

instead of `all` you can use `build` or `deploy`.

**StageSSIS.json**

```json
{
	"build" :{
		"dtproj" : "./StageSSIS/StageSSIS.dtproj",
		"config" : "Dev1-SQL"
	},
	"deploy":{
		"folder":"Stage",
		"ssisdb": {
			"driver"  :"net.sourceforge.jtds.jdbc.Driver",
			"url"     : "jdbc:jtds:sqlserver://dev1-sql:1433/SSISDB;useNTLMv2=true;domain=MYDOM",
			"user"    : "teamcity",
			"password": "xyz"
		}
	}
}

```
