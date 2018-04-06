/*  ssis deployer */

//the following two lines with grab definition for jdbc driver better to move on the level of teamcity script
@GrabConfig(systemClassLoader=true)
@Grab(group="net.sourceforge.jtds", module="jtds", version="1.3.1")

//
@Grab(group="commons-io", module="commons-io", version="2.6")

import org.apache.commons.io.input.BOMInputStream; //to remove BOM mark from packages
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import groovy.sql.Sql;
import groovy.xml.XmlUtil;

/**class to build and deploy ssis project (dtproj) through ispac package */
public class SSIS {
	private static void help(){
		println '''
			usage: groovy SSIS.groovy <mode> <path_to_json_conf> <path_to_ispac>
				where 
					mode: 
						build  - build ispac from dtproj
						deploy - deploy ispac on ssisdb
						all    - build and deploy
					path_to_json_conf:
						the json config in format:
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
									"password": "******"
								}
							},
							"ispac" :"./StageSSIS/bin/StageSSIS.ispac",
							"projName":"by default name of ispac file"
						}
			'''.stripIndent()
	}
	//no validation yet but could be
	//just init some defalt values
	private static void validateCtx(Map ctx){
		if(!ctx.ispac){
			def f = new File(ctx.build.dtproj)
			def n = f.getName().replaceAll( /([^\.]+)(\.\w+)?$/, '$1' )
			ctx.ispac = new File(f.getParentFile(), "bin/${n}.ispac").toString()
		}
		if(!ctx.projName)ctx.projName = new File(ctx.ispac).getName().replaceAll( /([^\.]+)(\.\w+)?$/, '$1')
	}
	

	public static void main(String [] arg){
		if(arg.size()!=2){
			help()
			System.exit(1)
			return
		}
		def ctx = new groovy.json.JsonSlurper().parse(new File(arg[1]))
		assert arg[0] in ["build","deploy", "all"]
		def mode = arg[0]
		
		if(mode in ["build", "all"]){
			buildISPAC(ctx);
		}
		if(mode in ["deploy", "all"]){
			deployISPAC(ctx);
		}
	}

	public static void deployISPAC(Map ctx){
		validateCtx(ctx)
		File ispac = new File(ctx.ispac)
		println "deploy $ispac"
		assert ctx.deploy.ssisdb

		ispac.withInputStream{rawIn->
			def query = """
			DECLARE @project_name nvarchar(128) = ${Sql.VARCHAR(ctx.projName)};
			DECLARE @folder_name nvarchar(128)  = ${Sql.VARCHAR(ctx.deploy.folder)};
			DECLARE @operation_id bigint        = -1;
			execute catalog.deploy_project @folder_name, @project_name, ${Sql.BLOB( rawIn.getBytes() )}, @operation_id output;
			select @operation_id as operation_id;
			"""
			Object ssisdb =  ctx.deploy.ssisdb
			if(ssisdb instanceof Sql){
					println( "  DONE: " + ssisdb.rows( query ) )
			}else{
				Sql.withInstance((Map)ssisdb){sql->
					println( "  DONE: " + sql.rows( query ) )
				}
			}
		}
		
	}

	//returns the file that references to created ispac file
	public static File buildISPAC(Map ctx){
		validateCtx(ctx)
		def SSIS=new groovy.xml.Namespace("www.microsoft.com/SqlServer/SSIS")
		def DTS = new groovy.xml.Namespace("www.microsoft.com/SqlServer/Dts")
		def projXml=new XmlParser().parse( new File(ctx.build.dtproj) )
		def manifestXml = projXml.DeploymentModelSpecificContent.Manifest[SSIS.Project][0]
		def configXml = projXml.Configurations.Configuration.find{it.Name.text()?.equalsIgnoreCase(ctx.build.config)}
		assert configXml
		assert manifestXml

		def projDir = new File(ctx.build.dtproj).getParentFile()
		File ispac = new File(ctx.ispac)
		println "build  ${ispac}"
		
		//CLEAN
		//delete ispac file
		ispac.delete()
		//create out directory
		ispac.getParentFile().mkdirs()
		//BUILD ISPAK
		new ZipOutputStream(ispac.newOutputStream()).withStream{zip->
			zip.setLevel(9) //max pack level..
			def paramsXml = new XmlParser().parse( new File(projDir, "Project.params") )
			def params = paramsXml[SSIS.Parameter].collectEntries{ 
				[ it.attributes()[SSIS.Name], it[SSIS.Properties][SSIS.Property].find{it.attributes()[SSIS.Name]=="Value"} ]
			}
			def configs = configXml.Options.ParameterConfigurationValues.ConfigurationSetting.findAll{it.Name.text().startsWith("Project::")}.collectEntries{ 
				[ it.Name.text().substring(9), it.Value.text()]
			}
			//modify params with configs values
			configs.each{k,v->
				params[k]?.setValue( v )
			}
			//convert params to usual Map<String,String> to simplify later usage
			params = params.collectEntries{ [it.getKey(), it.getValue()?.text()] }
			//remove connection parameters from manifest
			manifestXml[SSIS.DeploymentInfo][SSIS.ProjectConnectionParameters][0]?.setValue([])
			//store new project manifest into ispac
			add2zip(zip, "@Project.manifest"){ serializeNoDecl(manifestXml, it) }
			//process each comngr
			manifestXml[SSIS.ConnectionManagers][SSIS.ConnectionManager].collect{it.attributes()[SSIS.Name]}.each{conmgr->
				def conmgrXml=new XmlParser().parse(new File(projDir, conmgr))
				//connection string
				def cs = conmgrXml[DTS.PropertyExpression].find{it.attributes()[DTS.Name]=="ConnectionString"}?.text() ?: ""
				cs = evalProjParams(cs, params)
				cs = parseConnectionString(cs) //convert to map
				//database
				def db = conmgrXml[DTS.PropertyExpression].find{it.attributes()[DTS.Name]=="InitialCatalog"}?.text() ?: ""
				db = evalProjParams(db, params)
				//set db in the connection string
				if(cs && db)cs["Initial Catalog"]=db;
				//
				def pw = conmgrXml[DTS.PropertyExpression].find{it.attributes()[DTS.Name]=="Password"}?.text() ?: ""
				pw = evalProjParams(pw, params)
				//modify connection object
				if(cs)conmgrXml[DTS.ObjectData][DTS.ConnectionManager][0]?.attributes()?.put(DTS.ConnectionString, cs.collect{k,v->"$k=$v;"}.join(""))
				if(pw)conmgrXml[DTS.ObjectData][DTS.ConnectionManager][DTS.Password][0]?.setValue( pw )
				//put conmgr into ispac
				add2zip(zip, conmgr){ XmlUtil.serialize(conmgrXml, it) }
			}
			//put each package into ispac
			manifestXml[SSIS.Packages][SSIS.Package].collect{it.attributes()[SSIS.Name]}.each{pkg->
				add2zip(zip, pkg){ 
					it << new BOMInputStream(new File(projDir, pkg).newInputStream())
				}
			}
			add2zip(zip,"[Content_Types].xml"){ 
				it << '''
				<?xml version="1.0" encoding="utf-8"?>
				<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
					<Default Extension="dtsx" ContentType="text/xml" />
					<Default Extension="conmgr" ContentType="text/xml" />
					<Default Extension="params" ContentType="text/xml" />
					<Default Extension="manifest" ContentType="text/xml" />
				</Types>
				'''.stripIndent().trim().getBytes("UTF-8")
			}
			add2zip(zip,"Project.params"){
				XmlUtil.serialize(paramsXml, it)
			}
		}
		println "  DONE."
		return ispac
	}

	private static String evalProjParams(String s, Map projParams){
		return s.replaceAll('@\\[\\$Project::(\\w+)\\]'){t->
			//println "$t"
			projParams[t[1]]
		}
	}

	//serialize xml node without xml declaration
	private static void serializeNoDecl(Node x,OutputStream out){
		def w = new PrintWriter(out.newWriter("UTF-8"))
		def pw = new XmlNodePrinter(w)
		pw.setPreserveWhitespace(true)
		pw.print(x)
	}

	public static Map parseConnectionString(String s){
		if(!s)return [:];
		Map m = s.split(';').collectEntries{a->
			try{	
				(a=~/([^=]+)=(.*)/).find{true}.drop(1) 
			}catch(Throwable t){
				throw new RuntimeException("Can't parse connection string: `${s}` at `${a}` : ${t}")
			}
		}
		return m
	}

	@groovy.transform.CompileStatic
	static ZipOutputStream add2zip(ZipOutputStream zip, String path,Closure c){
		zip.putNextEntry(new ZipEntry(path));
		c.call(zip);
		zip.flush();
		zip.closeEntry();
		return zip;
	}

	/*
	def deployPacks(){
		Sql.withInstance(driver:"net.sourceforge.jtds.jdbc.Driver", url: "jdbc:jtds:sqlserver://dev1-sql.digagro.com:1433/SSISDB;useNTLMv2=true;domain=DIGAGRO", user:"teamcity", password:"New_opportunity",){sql->
			//xml namespace
			def SSIS=new groovy.xml.Namespace("www.microsoft.com/SqlServer/SSIS")
			def x=new XmlParser().parse(projSourceFile)
			def packFiles = x.DeploymentModelSpecificContent.Manifest[SSIS.Project][SSIS.Packages][SSIS.Package].collect{
				new File(projSourceFile.getParentFile(), it.attributes()[SSIS.Name])
			}
		
			packFiles = packFiles.take(1)
		
			String query = """
			DECLARE @project_name nvarchar(128) = ?;
			DECLARE @folder_name nvarchar(128)  = ?;
			DECLARE @packs catalog.Package_Table_Type;
			DECLARE @operation_id bigint=-1;
			INSERT INTO @packs VALUES ${ packFiles.collect{'(?,?)'}.join(', ') };
			execute catalog.deploy_packages @folder_name, @project_name, @packs, @operation_id output;
			select @operation_id as operation_id;
			"""
			println "deploy $packFiles"
			def params = [projName, projTargetPath] + packFiles.collectMany{
				[ it.getName().replaceAll(/\.\w*$/,""), Sql.BLOB( new BOMInputStream(it.newInputStream()).getBytes() ) ]
			}
			println sql.rows( query, params )
		}
	}
	*/
}
