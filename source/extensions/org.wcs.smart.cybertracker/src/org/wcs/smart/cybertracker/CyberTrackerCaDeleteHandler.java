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
package org.wcs.smart.cybertracker;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ICaDeleteHandler;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.util.PdaUtil;

/**
 * Delete handler for deleting all CyberTracker information attached 
 * to the conservation area.
 * <p>This deletes properties data and registry key</p>
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerCaDeleteHandler implements ICaDeleteHandler {

	public static final int EXECUTE_ORDER = 1;

	@Override
	public void beforeDelete(ConservationArea ca, Session session, IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.CyberTrackerCaDeleteHandler_DeleteProperties);
		deleteCTProperties(ca, session);
		monitor.subTask(Messages.CyberTrackerCaDeleteHandler_DeleteRegistryKey);
		deleteCTRegistryKey(ca);
	}

	private void deleteCTProperties(ConservationArea ca, Session session) {
		Query q = session.createQuery("delete from CyberTrackerPropertiesOption where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();

		q = session.createQuery("delete from CyberTrackerPropertiesProfile where conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
	}

	private void deleteCTRegistryKey(ConservationArea ca) throws Exception {
		try{
			PdaUtil.deleteRegistryKey(ca);
		}catch (Exception ex){
			CyberTrackerPlugIn.log("Could not delete registry key", ex); //$NON-NLS-1$
		}
	}
	
}
