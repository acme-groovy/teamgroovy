/**/
package groovyx.acme.teamcity.helpers;
import groovy.lang.Closure;
import java.io.File;
import java.io.Reader;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;


public class Strings{

	/** runs template */
	public static String asString(Object o) throws java.io.IOException{
		if(o==null){
			return null;
		}else if(o instanceof String){
			return (String)o;
		}else if(o instanceof Closure){
			return asString(((Closure)o).call());
		}else if(o instanceof File){
			return DefaultGroovyMethods.getText((File)o,"UTF-8");
		}else if(o instanceof Reader){
			return DefaultGroovyMethods.getText((Reader)o);
		}
		return o.toString();
	}
}
