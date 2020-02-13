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
package org.wcs.smart.paws.plugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.DeleteConservationAreaHandler;
import org.wcs.smart.ca.ICaDeleteHandler;

/**
 * Handler for deleting plugin specific data when the
 * Conservation Area is deleted.
 * 
 * @author Emily
 *
 */
public class DeleteCaHandler implements ICaDeleteHandler {

	/**
	 * This represents the order the plugin should be deleted in.  Higher orders
	 * are deleted first.  Lower orders deleted after.  So if this plugin must delete
	 * data before another plugin then the execute order needs to be higher than 
	 * the other plugin.
	 */
	//currently this is only requires to be executed before the core data is 
	//removed.
	public static final int EXECUTE_ORDER =  DeleteConservationAreaHandler.EXECUTE_ORDER + 1;

	
	@Override
	public void beforeDelete(ConservationArea ca, Session session, IProgressMonitor monitor) throws Exception {
		
		monitor.subTask("removing PAWS plugin data"); //$NON-NLS-1$
		
		Query<?>  q = session.createQuery("DELETE FROM PawsWorkspace WHERE conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("DELETE FROM PawsService WHERE conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();		
		
		q = session.createQuery("DELETE FROM PawsRun WHERE conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("DELETE FROM PawsQueryClass WHERE configuration IN (FROM PawsConfiguration WHERE conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
				
		q = session.createQuery("DELETE FROM PawsSimpleClass WHERE configuration IN (FROM PawsConfiguration WHERE conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("DELETE FROM PawsParameter WHERE configuration IN (FROM PawsConfiguration WHERE conservationArea = :ca)"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
		
		q = session.createQuery("DELETE FROM PawsConfiguration WHERE conservationArea = :ca"); //$NON-NLS-1$
		q.setParameter("ca", ca); //$NON-NLS-1$
		q.executeUpdate();
	}

}
