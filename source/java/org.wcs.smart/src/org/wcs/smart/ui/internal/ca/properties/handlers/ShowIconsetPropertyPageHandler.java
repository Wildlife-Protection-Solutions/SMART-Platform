/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.ui.internal.ca.properties.handlers;

import javax.inject.Named;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.IconFKManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.internal.ca.properties.IconsetPropertyPage;

/**
 * Handler for showing iconset property page
 * @author Emily
 * @since 1.0.0
 */
@SuppressWarnings("restriction")
public class ShowIconsetPropertyPageHandler extends ShowPropertyPageHandler {

	/**
	 * @param page
	 */
	public ShowIconsetPropertyPageHandler() {
		super(IconsetPropertyPage.class);
	}

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell) throws ExecutionException {
		
		try(Session session = HibernateManager.openSession()){
			IconFKManager.INSTANCE.dropIconFkConstraints(session);
		}catch (Exception ex) {
			//this shouldn't happen but for whatever reason the fk constraints can't be removed so
			//don't show the dialog.
			throw new ExecutionException(ex.getMessage());
		}
		
		try {
			super.execute(activeShell);
		}finally {
			try(Session session = HibernateManager.openSession()){
				IconFKManager.INSTANCE.createIconFkConstraints(session);
			}
		}	
	}
	
	// E3
	public static class ShowIconsetPropertyPageHandlerWrapper extends DIHandler<ShowIconsetPropertyPageHandler> {
		public ShowIconsetPropertyPageHandlerWrapper() {
			super(ShowIconsetPropertyPageHandler.class);
		}
	}
}
