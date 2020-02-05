/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.ui.views.QueryProxy;

/**
 * Label provider for profile queries 
 * 
 * @author Emily
 *
 */
public class IntelQueryLabelProvider extends OwnerDrawLabelProvider {
	
	public static final String PROFILE_KEYS = "PROFILES"; //$NON-NLS-1$
	
	private TableColumn tc;
	
	public IntelQueryLabelProvider(TableColumn tc) {
		this.tc = tc;
	}
	
	public String getText(Object element){
		if (element instanceof QueryProxy) return ((QueryProxy) element).getName();
		if (element instanceof AbstractIntelQuery) return ((AbstractIntelQuery)element).getName();
		return element.toString();
	}
	
	public Image getImage(Object element) {
		String queryType = null;
		if (element instanceof AbstractIntelQuery) {
			queryType = ((AbstractIntelQuery) element).getTypeKey();
		}else if (element instanceof QueryProxy) {
			queryType = ((QueryProxy)element).getTypeKey();
		}
		if (queryType == null) return null;
		
		return Resources.INSTANCE.getImage(queryType);
	}
	
	public Set<String> getProfiles(Object element){
		if (element instanceof AbstractIntelQuery) {
			return AbstractIntelQuery.convertFromProfileFilter( ((AbstractIntelQuery) element).getProfileFilter());
		}else if (element instanceof QueryProxy) {
			return AbstractIntelQuery.convertFromProfileFilter( ((QueryProxy) element).getProfileFilter());
		}
		return Collections.emptySet();
	}

	@Override
	protected void erase(Event event, Object element) {
	}
	
	@Override
	protected void measure(Event event, Object element) {
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void paint(Event event, Object element) {
		
		int x = event.getBounds().x;
		int y = event.getBounds().y;
		
		Image img = getImage(element);
		if (img != null) {
			event.gc.drawImage(img, x, y);
			x+= img.getBounds().width + 5;
		}
		String txt = getText(element);
		event.gc.drawText(txt,x,y+2, SWT.DRAW_TRANSPARENT);
		
		x = tc.getParent().getBounds().width;
		Set<String> allprofiles = getProfiles(element);
		Set<String> current = (Set<String>)tc.getParent().getData(PROFILE_KEYS);
		if (current == null) return;
		for (String ip : current)  {
			x -= 16+5;
			if (!allprofiles.contains(ip)) continue;
			Image i = Resources.INSTANCE.getProfileImage(ip);
			if (i == null) {
				event.gc.drawRectangle(x, y+2, 16, 16);
			}else {
				event.gc.drawImage(i, x, y+2);
			}
			
				
		}
		
	}
}
