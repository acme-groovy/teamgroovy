/**/
package groovyx.acme.teamcity.helpers;
import groovy.lang.Closure;
import groovy.lang.GString;
import java.io.File;
import java.io.Reader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;

import java.nio.charset.Charset;

public class Cast{
	static final String defaultEncoding = "UTF-8";

	/** converts object to string. or reads a resource as string if it's file,stream or reader.
	  * for closures evaluates them and tries to cast result to string
	  */
	public static String asString(Object o) throws IOException{
		if(o==null){
			return null;
		}else if(o instanceof String){
			return (String)o;
		}else if(o instanceof Closure){
			return asString(((Closure)o).call());
		}else if(o instanceof File){
			return ResourceGroovyMethods.getText((File)o,defaultEncoding);
		}else if(o instanceof URL){
			return ResourceGroovyMethods.getText((URL)o);
		}else if(o instanceof Reader){
			return IOGroovyMethods.getText((Reader)o);
		}else if(o instanceof InputStream){
			return IOGroovyMethods.getText((InputStream)o,defaultEncoding);
		}
		return o.toString();
	}
	
	public static Reader asReader(Object o) throws IOException{
		if(o==null){
			return new StringReader("");
		}else if(o instanceof String){
			return new StringReader((String)o);
		}else if(o instanceof GString){
			return new StringReader(((GString)o).toString());
		}else if(o instanceof Closure){
			return asReader(((Closure)o).call());
		}else if(o instanceof File){
			return ResourceGroovyMethods.newReader((File)o,defaultEncoding);
		}else if(o instanceof URL){
			return ResourceGroovyMethods.newReader((URL)o);
		}else if(o instanceof Reader){
			return (Reader)o;
		}else if(o instanceof InputStream){
			return IOGroovyMethods.newReader((InputStream)o,defaultEncoding);
		}
		throw new IOException("Can't cast "+o.getClass()+"to Reader");
	}
}
