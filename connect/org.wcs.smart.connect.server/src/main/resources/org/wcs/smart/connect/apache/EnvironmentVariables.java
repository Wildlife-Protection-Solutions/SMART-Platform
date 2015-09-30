package org.wcs.smart.connect.apache;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public enum EnvironmentVariables {
	INSTANCE;
	
	public enum Variable{
		DATASTORE_LOCATION("filestorelocation"),
		NUM_BACK_THREADS("number_background_threads");
		
		public String key;
		
		private Variable(String key){
			this.key = key;
		}
	}
	
	private Context ctx;
	public Object getEnvironmentVairable(Variable v) throws NamingException{
		return getContext().lookup(v.key);
	}
	
	private Context getContext() throws NamingException{
		if (ctx == null){
			ctx = (Context)(new InitialContext()).lookup("java:comp/env");
		}
		return ctx;
	}
}
