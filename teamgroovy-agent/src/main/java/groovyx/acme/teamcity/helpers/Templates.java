/**/
package groovyx.acme.teamcity.helpers;

import java.util.Map;
import groovy.lang.Closure;
import groovy.text.SimpleTemplateEngine;

public class Templates{

	/** runs template */
	public static String make(Map binding, Object tpl) throws ClassNotFoundException, java.io.IOException{
		return new SimpleTemplateEngine()
			.createTemplate( Cast.asReader(tpl) )
			.make(binding)
			.toString();
	}
}
