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
package org.wcs.smart.dataentry.dialog.composite;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider.CmRootNode;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.DisplayMode;

/**
 * Listener for cascading display mode to children nodes/attributes 
 * 
 * @author Emily
 *
 */
public class CascadeDisplayModeListener implements Listener {

	private DisplayModeComboViewer viewer;
	private AbstractInfoComposite node;
	private CmAttribute attribute;
	
	public CascadeDisplayModeListener(DisplayModeComboViewer viewer, AbstractInfoComposite node){
		this.viewer = viewer;
		this.node = node;
	}
	
	public CascadeDisplayModeListener(DisplayModeComboViewer viewer, CmAttribute node){
		this.viewer = viewer;
		this.attribute = node;
	}
	
	@Override
	public void handleEvent(Event event) {
		DisplayMode newMode = viewer.getSelectedDisplayMode();
		if (node != null) {
			if (node.getSourceObject() instanceof CmNode) {
				
				if (!MessageDialog.openQuestion(Display.getDefault().getActiveShell(),Messages.CascadeDisplayModeListener_WarningTitle,Messages.CascadeDisplayModeListener_WarningMsg)) {
					return;
				}
				
				CmNode n = (CmNode)node.getSourceObject();
				processNode(n, newMode);
			}else if (node.getSourceObject() instanceof CmRootNode) {
				CmRootNode root = (CmRootNode)node.getSourceObject();
				List<CmNode> nodes = root.getModel().getNodes();
				for (CmNode n : nodes) {
					processNode(n, newMode);
				}
				
			}
		}else if (attribute != null) {
			processAttribute(attribute, newMode);
		}
	}
	
	private void processNode(CmNode node, DisplayMode newMode) {
		node.setDisplayMode(newMode);
		processAttributes(node, newMode);
		for (CmNode kid : node.getChildren()) {
			processNode(kid, newMode);
		}
	}
	
	private void processAttributes(CmNode node, DisplayMode newMode) {
		if (node.getCmAttributes() == null) return;
		for (CmAttribute a : node.getCmAttributes()) {
			processAttribute(a, newMode);
		}
	}
	
	private void processAttribute(CmAttribute node, DisplayMode newMode) {
		if (node.getConfig() != null) node.getConfig().setDisplayMode(newMode);
		
		if (node.getCurrentTree() != null) {
			List<CmAttributeTreeNode> toProcess = new ArrayList<>();
			toProcess.addAll(node.getCurrentTree());
			while(!toProcess.isEmpty()) {
				CmAttributeTreeNode p = toProcess.remove(0);
				p.setDisplayMode(newMode);
				if (p.getChildren() != null)toProcess.addAll(p.getChildren());
			}
		}
	}

}
