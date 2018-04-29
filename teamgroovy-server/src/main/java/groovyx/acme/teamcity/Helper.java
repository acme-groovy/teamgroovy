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

import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.Parameter;
import jetbrains.buildServer.util.Option;

import jetbrains.buildServer.controllers.admin.projects.BuildTypeForm;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;
import java.util.Enumeration;
import java.util.TreeSet;

import javax.servlet.jsp.PageContext;

public class Helper {
	private static String normalizeName(String name){
		if(name==null)return null;
		String prefix = null;
		
		int pos = name.indexOf('.');
		if(pos>0){
			prefix = name.substring(0,pos);
			name = name.substring(pos+1);
		}
		
		if(prefix!=null && name.matches("^[^\'\"]+$")){
			if( name.matches("^[a-zA-Z_][a-zA-Z_0-9]*$") ){
				return prefix + "." + name;
			}else{
				return prefix + ".'" + name + "'";
			}
		}
		return null;
	}
	
	private static String normalizePfx(String name){
		if( !name.startsWith("env.") && !name.startsWith("system.") ){
			return "config."+name;
		}
		return name;
	}
	
	public static String paramsAsJson(SBuildType buildType){
		TreeSet<String> list = new TreeSet();
		
		for(Parameter p : buildType.getParametersCollection())                 { list.add( normalizePfx(p.getName()) ); }
		for(String p    : buildType.getParametersProvider().getAll().keySet()) { list.add( normalizePfx(p) ); }
		
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for(String s : list){
			s=normalizeName(s);
			if(s!=null){
				if(sb.length()>1)sb.append(", ");
				sb.append('"');
				sb.append(s); 
				sb.append('"');
			}
		}
		sb.append(']');
		return sb.toString();
	}
	
	public static String paramsAsJson(PageContext ctx){
		try{
			//request scope attributes
			SBuildServer  server = (SBuildServer) ctx.getAttribute("serverTC", 2);
			BuildTypeForm buildForm = (BuildTypeForm) ctx.getAttribute("buildForm", 2);
			if(server==null)throw new Exception("can't get `serverTC` request attribute");
			if(buildForm==null)throw new Exception("can't get `buildForm` request attribute");
			
			SBuildType buildType = server.getProjectManager().findBuildTypeByExternalId( buildForm.getExternalId() );
			if(buildType==null)throw new Exception("can't findBuildTypeByExternalId: "+buildForm.getExternalId());
			
			System.out.println("id="+buildForm.getExternalId());
			System.out.println("buildType="+buildType);
			
			return paramsAsJson(buildType);
		}catch(Throwable t){
			return "[\""+ t.toString().replace('"','\'') +"\"]";
		}
	}
	
	
	public static String attributes(PageContext ctx){
		StringBuilder sb = new StringBuilder();
		for(int scope=1;scope<=4;scope++){
			sb.append("\n---- scope = "+scope+"\n");
			
			try{
				Enumeration<String> attrs = ctx.getAttributeNamesInScope(scope);
			
				for(Object n : Collections.list(attrs) ){
					sb.append(n);
					sb.append('\n');
				}
			}catch(Throwable t){
				sb.append(t.toString());
				sb.append('\n');
			}
		}
		return sb.toString();
	}
	
}
