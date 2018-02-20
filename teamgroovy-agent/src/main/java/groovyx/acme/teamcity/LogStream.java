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

import jetbrains.buildServer.agent.BuildProgressLogger;
import java.io.ByteArrayOutputStream;

public class LogStream extends java.io.OutputStream {
	public static final int LEVEL_INFO=0;
	public static final int LEVEL_WARN=1;
	public static final int LEVEL_ERR=2;

	private BuildProgressLogger log;
	private int level;
	private ByteArrayOutputStream buf=new ByteArrayOutputStream(128);

	public LogStream(BuildProgressLogger log, int level){
		this.log = log;
		this.level = level;
	}

    @Override
	public void write(int b)throws java.io.IOException{
		if(b==10 || b==13) {
			/* CR or LF */
			if(buf.size()>0){
				String msg = buf.toString("UTF-8");

				if(level==LEVEL_INFO)log.message( msg );
				else if(level==LEVEL_WARN)log.warning( msg );
				else if(level==LEVEL_ERR)log.error( msg );

				buf.reset();
			}
		}else{
			buf.write(b);
		}
	}

}
