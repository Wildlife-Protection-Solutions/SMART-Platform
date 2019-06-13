/*
 * Copyright (C) 2019 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.connect.api.noa;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.exceptions.GeneralExceptionMapper;
import org.wcs.smart.connect.exceptions.SmartConnectExceptionMapper;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * SMART Connect REST application apis that
 * are available with no authorization - it is up to 
 * the individual apis to perform authorization
 * as required.
 * 
 * 
 * @author Emily
 *
 */
@ApplicationPath(ConnectRESTApplication.PATH_SEPERATOR + "noa")

@OpenAPIDefinition(servers = {@Server(url = "https://<connectserver>/connect/noa/") }, 
	info = @Info(title = "SMART Connect API", description = "SMART Connect API avaliable without login", version = "7.0"))
public class ConnectNoaRESTApplication extends Application {

	public static final String APIKEY_QUERY_PARAM = "api_key";
	public static final String APIKEY_HEADER_PARAM = "X-API-KEY";
	
	public static final String PATH_SEPERATOR = "/"; //$NON-NLS-1$

	public static final String NO_AUTH_PATH = PATH_SEPERATOR + "noa" + PATH_SEPERATOR ; //$NON-NLS-1$

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> resources = new HashSet<>();
	    resources.add(CyberTrackerNoa.class);
	    
	    resources.add(SmartConnectExceptionMapper.class);
		resources.add(GeneralExceptionMapper.class);
		
	    return resources;
	}
}
