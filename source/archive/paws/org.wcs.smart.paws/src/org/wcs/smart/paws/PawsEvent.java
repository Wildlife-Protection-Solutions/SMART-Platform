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
package org.wcs.smart.paws;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.wcs.smart.paws.model.PawsRun;

/**
 * PAWS events accessible via IEventBroker interface
 * 
 * @author Emily
 *
 */
public class PawsEvent {

	public static final String PAWS_CONFIG_ALL = "PAWSCONFIG/*"; //$NON-NLS-1$
	public static final String PAWS_CONFIG_NEW = "PAWSCONFIG/NEW"; //$NON-NLS-1$
	public static final String PAWS_CONFIG_MODIFY = "PAWSCONFIG/EDIT"; //$NON-NLS-1$
	public static final String PAWS_CONFIG_DELETE = "PAWSCONFIG/DELETE"; //$NON-NLS-1$
	
	public static final String PAWS_RUN_ALL = "PAWSRUN/*"; //$NON-NLS-1$
	public static final String PAWS_RUN_NEW = "PAWSRUN/NEW"; //$NON-NLS-1$
	public static final String PAWS_RUN_MODIFY = "PAWSRUN/EDIT"; //$NON-NLS-1$
	public static final String PAWS_RUN_DELETE = "PAWSRUN/DELETE"; //$NON-NLS-1$
	
	
	public static void fireModified(PawsRun run){
		EclipseContextFactory.getServiceContext(PawsPlugIn.getDefault().getBundle().getBundleContext()).get(IEventBroker.class)
			.post(PawsEvent.PAWS_RUN_MODIFY, Collections.singleton(run));
	}
	
	public static void fireModified(Collection<PawsRun> runs){
		EclipseContextFactory.getServiceContext(PawsPlugIn.getDefault().getBundle().getBundleContext()).get(IEventBroker.class)
			.post(PawsEvent.PAWS_RUN_MODIFY, runs);
	}
}
