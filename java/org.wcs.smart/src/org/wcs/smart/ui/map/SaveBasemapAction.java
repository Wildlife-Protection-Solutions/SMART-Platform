/* uDig - User Friendly Desktop Internet GIS client
 * http://udig.refractions.net
 * (C) 2004-2008, Refractions Research Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package org.wcs.smart.ui.map;

import java.util.Iterator;
import java.util.List;

import net.refractions.udig.project.internal.impl.ProjectRegistryImpl;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class SaveBasemapAction implements IViewActionDelegate {

	@Override
	public void run(IAction action) {
		// TODO Auto-generated method stub

		ProjectRegistryImpl.getProjectRegistry().eResource();
		
		 List<Resource> resources = ProjectRegistryImpl.getProjectRegistry().eResource().getResourceSet().getResources();
         for( Iterator<Resource> iter = resources.iterator(); iter.hasNext(); ) {
             Resource resource = (Resource) iter.next();
             System.out.println("break");
         }
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IViewPart view) {
		// TODO Auto-generated method stub

	}

}
