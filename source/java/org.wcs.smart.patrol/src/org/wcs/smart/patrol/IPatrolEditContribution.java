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
package org.wcs.smart.patrol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.hibernate.Session;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;


/**
 * Contribution management when a patrol is split or merged.
 * 
 * @author Emily
 *
 */
public interface IPatrolEditContribution {

	public static final String EXTENSION_ID = "org.wcs.smart.patrol.contribution"; //$NON-NLS-1$
	
	/**
	 * Called when the patrol is split from one patrol into multiple patrols.
	 * Called once for each new patrol created from the original patrol.
	 * 
	 * @param originalPatrol the original patrol
	 * @param newPatrol the new patrol
	 * 
	 */
	public void splitPatrol(Session s, Patrol originalPatrol, Patrol newPatrol);
	
	/**
	 * Called when two patrols are merged together or split apart. Both these actions create
	 * new legs and this allows details associated with the leg to be copied to the new leg
	 * 
	 * @param currentLeg
	 * @param toLeg
	 * @param session
	 * 
	 * @since 8.1.0 
	 */
	//added to support the maintenance of smart mobile device ids
	public default void mergePatrolMovePatrolLeg(PatrolLeg currentLeg, PatrolLeg toLeg, Session session) {}

	
	/**
	 * find all contributions registered via extensions
	 * @return
	 */
	public static List<IPatrolEditContribution> findContributions(){

		List<IPatrolEditContribution> items = new ArrayList<IPatrolEditContribution>();
		if (Platform.getExtensionRegistry() == null) return
		Collections.emptyList();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IPatrolEditContribution.EXTENSION_ID);
		try {
		    for (IConfigurationElement e : config) {
		        if (e.getName().equals("edit")){ //$NON-NLS-1$
		            IPatrolEditContribution page = (IPatrolEditContribution)e.createExecutableExtension("class"); //$NON-NLS-1$
		            items.add(page);
		        }
		    }
		}catch (Exception ex){
		         SmartPatrolPlugIn.displayLog(Messages.CreatePatrolWizard_ErrorCreatingWizardPages, ex);
		    return null;
		}
		return items;
	} 
}
