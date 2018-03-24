/**/
package groovyx.acme.teamcity.helpers;

import groovy.lang.Closure;
import groovy.lang.Tuple;

import groovy.sql.Sql;
import groovy.sql.SqlWithParams;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class SqlHelper implements Driver{
	private static Pattern goDelim = Pattern.compile("(?i)^GO\\s*$");
	private static Pattern slashDelim = Pattern.compile("(?i)^/\\s*$");
	private static Pattern spaces = Pattern.compile("(?i)^\\s*$");


	//static Map<long,Object> delegate = new java.util.concurrent.ConcurrentHashMap<>();
	Driver driver = null;
	
	/** splits resource stream, file or reader to commands and calls closure for each command.
	 * delimiter is a regexp that should fully match one line. the matched line not considered as a command.
	 * if the last command not ended with a delimiter then exception will be thrown.
	 */
	public static void eachCommand(Object batch, Pattern delim, Closure c) throws IOException {
		BufferedReader r = Cast.asReader(batch);
		StringBuilder cmd = new StringBuilder();
		String line;
		while( (line = r.readLine())!=null ){
			if( delim.matcher(line).matches() ){
				//got a delimiter
				c.call(cmd.toString());
				cmd.setLength(0);
			} else {
				if(cmd.length()>0)cmd.append("\r\n");
				cmd.append(line);
			}
		}
		if( !spaces.matcher(cmd).matches() )throw new RuntimeException("The last command was not processed: \n"+cmd);
	}
	
	
	/** splits resource stream, file or reader to commands with `GO` delimiter line and calls closure for each command.
	 */
	public static void eachGoCommand(Object batch, Closure c)  throws IOException {
		eachCommand( batch, goDelim, c );
	}
	
	/** splits resource stream, file or reader to commands with `/` delimiter line and calls closure for each command.
	 */
	public static void eachSlashCommand(Object batch, Closure c) throws IOException {
		eachCommand( batch, slashDelim, c );
	}

	public static void withInstance(Map<String, Object> args, Closure c) throws Throwable {
		Object driver = args.get("driver");
		String url = (String)args.get("url");
		if(driver==null)throw new RuntimeException("The paramener `driver` is required.");
		if(url==null)throw new RuntimeException("The paramener `url` is required.");
		
		Sql sql = null;
		SqlHelper helper = null;
		Properties props = new Properties();
		for(Map.Entry<String,Object> e : args.entrySet())
			if( !"driver".equals(e.getKey()) && !"url".equals(e.getKey()) )
				props.setProperty(e.getKey(), Cast.asString(e.getValue()));
		
		try {
			if(driver instanceof CharSequence){
				Class cdriver = Class.forName( ((CharSequence)driver).toString(), true, Thread.currentThread().getContextClassLoader() );
				driver = cdriver.newInstance();
			}
			helper = new SqlHelper((Driver)driver);
			DriverManager.registerDriver(helper);
			
			sql = new WSql( DriverManager.getConnection(url, props) );
			c.call(sql);
			if(!sql.getConnection().getAutoCommit())sql.commit();
		} catch(Throwable t) {
			try {
				if (sql != null) {
					if(!sql.getConnection().getAutoCommit())sql.rollback();
				}
			}catch(SQLException e){}
			throw t; 
			
		} finally {
			try {
				if (sql != null) sql.close();
			}catch(Throwable e){}
			if(helper != null)DriverManager.deregisterDriver(helper);
		}
	}
	public SqlHelper(Driver driver){
		this.driver = driver;
	}
	@Override
	public boolean acceptsURL(String u) throws SQLException {
		return this.driver.acceptsURL(u);
	}
	@Override
	public Connection connect(String u, Properties p) throws SQLException {
		return this.driver.connect(u, p);
	}
	@Override
	public int getMajorVersion() {
		return this.driver.getMajorVersion();
	}
	@Override
	public int getMinorVersion() {
		return this.driver.getMinorVersion();
	}
	@Override
	public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
		return this.driver.getPropertyInfo(u, p);
	}
	@Override
	public boolean jdbcCompliant() {
		return this.driver.jdbcCompliant();
	}
	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException{
		return this.driver.getParentLogger();
	}
	
	static class WSql extends Sql {
		public WSql(Connection con){
			super(con);
		}
		@Override
		public SqlWithParams checkForNamedParams(String sql, List<Object> params) {
			SqlWithParams preCheck = buildSqlWithIndexedProps(sql);
			if (preCheck == null) {
				//the next line is the only change to original groovy code to fix error when sql has no named parameters, but params contains a map
				if(params.size()==1 && params.get(0) instanceof Map)params=new ArrayList();
				return new SqlWithParams(sql, params);
			}

			List<Tuple> indexPropList = new ArrayList<Tuple>();
			for (Object next : preCheck.getParams()) {
				indexPropList.add((Tuple) next);
			}
			return new SqlWithParams(preCheck.getSql(), getUpdatedParams(params, indexPropList));
		}
	}	
}
