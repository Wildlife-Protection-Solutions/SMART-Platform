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
package org.wcs.smart.cybertracker.patrol.navigation;

import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.model.NavigationTarget;
import org.wcs.smart.cybertracker.navigation.ui.INavigationLayerTargetProvider;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.TrackPart;

/**
 * Navigation layer target provider for patrol tracks.
 * 
 * @author Emily
 *
 */
public class PatrolTrackNavigationTargetProvider implements INavigationLayerTargetProvider {

	private PatrolTrackWizardPage page1;
	
	@Override
	public String getTypeName() {
		return Messages.PatrolTrackNavigationTargetProvider_ProviderType;
	}

	@Override
	public Image getImage() {
		return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL32_ICON);
	}
	
	@Override
	public List<WizardPage> getPages() {
		if (page1 == null) {
			page1 = new PatrolTrackWizardPage();
		}
		return Collections.singletonList(page1);
	}

	@Override
	public List<NavigationTarget> getTargets(IProgressMonitor monitor) {
		
		List<TrackPart> parts = page1.getTracks();
		List<NavigationTarget> targets = new ArrayList<>();
		
		for (TrackPart part : parts) {
			try {
				StringBuilder sb = new StringBuilder();
				sb.append( part.getTrack().getPatrolLegDay().getPatrolLeg().getPatrol().getId() );
				sb.append( " - "); //$NON-NLS-1$
				sb.append(  DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(part.getTrack().getPatrolLegDay().getDate()));
				try {
					if (part.getTrack().getTrackParts().size() > 1)
						sb.append( MessageFormat.format(" ({0}) ", + part.getIndex())); //$NON-NLS-1$
				}catch (Exception ex) {
					CyberTrackerPlugIn.log(ex.getMessage(), ex);
				}
				
				NavigationTarget target = new NavigationTarget();
				target.setGeometry(part.getLineString());
				target.setId(sb.toString());
				targets.add(target);
				
			} catch (Exception e) {
				CyberTrackerPlugIn.log(e.getMessage(), e);
			}	
		}
		
		return targets;
	}

	@Override
	public boolean canFinish() {
		return page1.isPageComplete();
	}

}
