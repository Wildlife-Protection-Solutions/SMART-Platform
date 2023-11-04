/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.api;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.wcs.smart.connect.exceptions.GeneralExceptionMapper;
import org.wcs.smart.connect.exceptions.SmartConnectExceptionMapper;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * SMART Connect REST application available that requires
 * login authorization before access is allowed.
 * 
 * @author Emily
 *
 */
@ApplicationPath(ConnectRESTApplication.PATH_SEPERATOR + ConnectRESTApplication.APP_PATH)
@OpenAPIDefinition(servers = {@Server(url = "https://<connectserver>/connect/api/") }, 
info = @Info(title = "SMART Connect API", description = "SMART Connect API", version = "7.0"))
public class ConnectRESTApplication extends Application {
	public static final String PATH_SEPERATOR = "/"; //$NON-NLS-1$
	
	public static final String APP_PATH = "api"; //$NON-NLS-1$
	
	public static final String ALL_APP_PATH = PATH_SEPERATOR + APP_PATH + PATH_SEPERATOR + "*"; //$NON-NLS-1$
	
	public static final String SERVLET_PATH = PATH_SEPERATOR + "connect" + PATH_SEPERATOR ; //$NON-NLS-1$
	
	
	public static final String UTF8 = "UTF-8"; //$NON-NLS-1$

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> resources = new HashSet<>();
		resources.add(ConnectAlert.class);
		resources.add(ConnectAlertFilterDefault.class);
		resources.add(ConnectMapLayers.class);
		resources.add(ConnectStyleConfiguration.class);
		resources.add(ConnectUser.class);
		resources.add(ConnectUserAction.class);
		resources.add(ConservationAreas.class);
		resources.add(CorsFeature.class);
		resources.add(CyberTracker.class);
		resources.add(DashboardBetaApi.class);
		resources.add(DataModelApi.class);
		resources.add(DataApi.class);
		resources.add(DataQueue.class);
		resources.add(DataQueueEventService.class);
		resources.add(DesktopUser.class);
		resources.add(QueryApi.class);
		resources.add(CustomQueryApi.class);
		resources.add(QueryTypeApi.class);
		resources.add(QuicklinkApi.class);
		resources.add(ReportApi.class);
		resources.add(SharedLinkApi.class);
		resources.add(Uploader.class);
		resources.add(SmartCollectApi.class);
		resources.add(BasemapTileServer.class);
		
		resources.add(SmartConnectExceptionMapper.class);
		resources.add(GeneralExceptionMapper.class);
		
		resources.add(ObjectMapperContextResolver.class);
		
		resources.add(SmartInfo.class);
		return resources;
	}
	 
	
	
}
