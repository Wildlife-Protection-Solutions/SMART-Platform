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
package org.wcs.smart.connect.query.engine.i2;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.birt.report.engine.api.HTMLRenderContext;
import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.birt.BirtConstants;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.connect.report.query.ServerSmartConnection;
import org.wcs.smart.connect.security.AdvIntelAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.i2.birt.datasource.AbstractIntelBirtConnection;

/**
 * Advanced intelligence BIRT Connection
 * @author Emily
 *
 */
@SuppressWarnings("deprecation")
public class IntelConnection extends AbstractIntelBirtConnection {
	
	@Override
	public void openSession() {
		localSession = (Session) appContext.get(BirtConstants.SESSION_PARAM);
	}

	@Override
	public void closeSession() {
	}

	@Override
	public String decryptAttachment(ISmartAttachment attachment) {
		if (attachment == null) return null;
		
		Path imageOutDir = getImageOutputDirectory();
		Path imageFile = null;
		try {
			if (imageOutDir == null) {
				imageFile = EncryptUtils.decryptAttachment(attachment);
			}else {
				imageFile = imageOutDir.resolve(attachment.getFilename());
				imageFile = EncryptUtils.uniqueFile(imageFile);
				EncryptUtils.decryptAttachment(attachment, imageFile);
			}
			attachmentFiles.add(imageFile);
			
			if (imageFile.startsWith(Paths.get(SmartContext.INSTANCE.getFilestoreLocation()))) {
				return imageFile.toAbsolutePath().toUri().toString();
			}else {
				if (isHtml()) {
					return getImageUrl() + "/" + imageOutDir.relativize(imageFile).toString(); //$NON-NLS-1$
				}else {
					return imageFile.toAbsolutePath().toUri().toString();	
				}
			}
		} catch (Exception e) {
			//we are going to eat this; likely an issue decrypting the attachment 
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<ConservationArea> getConservationAreas() {
		Object x = appContext.get(ServerSmartConnection.CCAA_FILTER_KEY);
		if (x == null) return null;
		if (x instanceof ConservationArea){
			return Collections.singleton((ConservationArea)x);
		}
		return (Collection<ConservationArea>)x;
	}

	/**
	 * 
	 * @return the image output directory supplied in the app context.  Will
	 * return null if not specified
	 */
	@Override
	protected Path getImageOutputDirectory() {
		Object ops = appContext.get("HTML_RENDER_CONTEXT"); //$NON-NLS-1$
		if (ops == null) return null;
		HTMLRenderContext context = (HTMLRenderContext)ops;
		if (context.getImageDirectory() == null) return null;
		return Paths.get( context.getImageDirectory() );		
	}
	
	/**
	 * 
	 * @return the image output directory supplied in the app context.  Will
	 * return null if not specified
	 */
	
	protected String getImageUrl() {
		Object ops = appContext.get("HTML_RENDER_CONTEXT"); //$NON-NLS-1$
		if (ops == null) return null;
		HTMLRenderContext context = (HTMLRenderContext)ops;
		if (context.getBaseImageURL() == null) return null;
		return context.getBaseImageURL();		
	}
	/**
	 * If the output type is html
	 * @return
	 */
	@Override
	protected boolean isHtml() {
		if (appContext == null) return false;
		Object ctx = appContext.get("HTML_RENDER_CONTEXT"); //$NON-NLS-1$
		if (ctx == null) return false;
		if (!(ctx instanceof HTMLRenderContext)) return false;
		HTMLRenderContext cc = (HTMLRenderContext)ctx;
		return cc.getRenderOption().getOutputFormat().equalsIgnoreCase(HTMLRenderOption.HTML);
	}
	
	@Override
	public boolean hasPermission(Permission permission) {
		if (appContext == null) return true;
		String currentUser = (String)appContext.get(ServerSmartConnection.CURRENT_USER_KEY);
		if (currentUser == null) return false;
		
		switch(permission){
		case ENTITY:
			return SecurityManager.INSTANCE.canAccess(localSession, currentUser, AdvIntelAction.VIEWDATA_KEY);
		case QUERY:
			return SecurityManager.INSTANCE.canAccess(localSession, currentUser, AdvIntelAction.RUNQUERY_KEY);
		case RECORD:
			return SecurityManager.INSTANCE.canAccess(localSession, currentUser, AdvIntelAction.VIEWDATA_KEY);
		}
		return false;
	}

}
