/**/
package groovyx.acme.teamcity.helpers;

import groovy.lang.Closure;
import groovy.sql.Sql;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

import java.util.regex.Pattern;

public class SqlHelper implements Driver{
	private static Pattern goDelim = Pattern.compile("(?i)[\\r\\n]GO[\\r\\n$]");
	private static Pattern slashDelim = Pattern.compile("(?i)[\\r\\n]/[\\r\\n$]");

	//static Map<long,Object> delegate = new java.util.concurrent.ConcurrentHashMap<>();
	Driver driver = null;
	
	/** splits batch */
	public static void eachCommand(CharSequence batch, Pattern delim, Closure c) {
		String ss[] = delim.split(batch);
		for(String cmd: ss){
			cmd=cmd.trim();
			if(cmd.length()>0)c.call(cmd);
		}
	}
	public static void eachGoCommand(CharSequence batch, Closure c) {
		eachCommand( batch, goDelim, c );
	}
	public static void eachSlashCommand(CharSequence batch, Closure c) {
		eachCommand( batch, slashDelim, c );
	}

	public static void withInstance(Map<String, Object> args, Closure c) throws Throwable {
		Object driver = args.get("driver");
		if(driver==null)throw new RuntimeException("The paramener `driver` is required.");
		
		Sql sql = null;
		SqlHelper helper = null;
		try {
			if(driver instanceof CharSequence){
				Class cdriver = Class.forName( ((CharSequence)driver).toString(), true, Thread.currentThread().getContextClassLoader() );
				driver = cdriver.newInstance();
			}
			if(driver instanceof Driver){
				args = new HashMap(args);
				args.remove("driver");
				helper = new SqlHelper((Driver)driver);
				DriverManager.registerDriver(helper);
			}
			sql = Sql.newInstance(args);
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
}
