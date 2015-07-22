package org.wcs.smart.connect.api;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;


@ApplicationPath(ConnectRESTApplication.PATH_SEPERATOR + ConnectRESTApplication.APP_PATH)
public class ConnectRESTApplication extends Application {
	public static final String PATH_SEPERATOR = "/"; //$NON-NLS-1$
	
	public static final String APP_PATH = "api"; //$NON-NLS-1$
	
	public static final String SERVLET_PATH = PATH_SEPERATOR + "connect" + PATH_SEPERATOR ; //$NON-NLS-1$
	
	public static final String UTF8 = "UTF-8"; //$NON-NLS-1$
	
	
}
