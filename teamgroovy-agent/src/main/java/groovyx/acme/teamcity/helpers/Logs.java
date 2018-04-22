/**/
package groovyx.acme.teamcity.helpers;

import groovy.lang.Closure;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.messages.DefaultMessagesInfo;

public class Logs{

	/** runs template */
	public static Object section(BuildProgressLogger log, String name, String description, Closure c) {
		String type = DefaultMessagesInfo.BLOCK_TYPE_TARGET;
		log.activityStarted(name, description, type);
		try {
			return c.call();
		} finally {
			log.activityFinished( name, type );
		}
	}
}
