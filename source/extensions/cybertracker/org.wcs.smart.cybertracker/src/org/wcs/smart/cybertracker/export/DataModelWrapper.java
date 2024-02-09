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
package org.wcs.smart.cybertracker.export;

import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.dataentry.dialog.ConfigurableModelFactory;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Class is a lazy wrapper around original datamodel
 * to allow exporting it to CyberTracker application.
 * 
 * WARNING: uuids are generated randomly for all nodes, attribute listitems and tree
 * nodes. The new CT verion requires ID's for all nodes so after converting
 * the data model we apply random uuids to all nodes, attributes, listitems and tree
 * nodes.
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class DataModelWrapper {
	
	private ConfigurableModel model;

	/**
	 * 
	 * @param session
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	 * to call done() on the given monitor
	 * @return
	 */
	public ConfigurableModel buildConfigurableModel(Session session, IProgressMonitor monitor) {
		if (model == null) {
			model = ConfigurableModelFactory.createModelFromDataModel(Messages.DataModelWrapper_Name, session, monitor);
			assignIds(model);
		}
		return model;
	}
	
	private void assignIds(ConfigurableModel model) {
		model.getNodes().forEach(node->processNode(node));
		
	}
	
	private void processNode(CmNode node) {
		if (node.getUuid() == null) node.setUuid(UUID.randomUUID());
		
		if (node.getCmAttributes() != null) {
			node.getCmAttributes().forEach(a->processAttribute(a));
		}
		if (node.getChildren() != null) {
			node.getChildren().forEach(kid->processNode(kid));
		}
	}
	
	private void processAttribute(CmAttribute attribute) {
		if (attribute.getUuid() == null) attribute.setUuid(UUID.randomUUID());
		if (attribute.getConfig() != null) attribute.getConfig().setUuid(UUID.randomUUID());
		
		if (attribute.getCurrentList() != null) {
			attribute.getCurrentList().forEach(li->{
				if (li.getUuid() == null) li.setUuid(UUID.randomUUID());
			});
		}
		
		if (attribute.getCurrentTree() != null) {
			attribute.getCurrentTree().forEach(tr->processTreeNode(tr));
		}
	}
	
	private void processTreeNode(CmAttributeTreeNode tree) {
		if (tree.getUuid() == null) tree.setUuid(UUID.randomUUID());
		if (tree.getChildren() != null) {
			tree.getChildren().forEach(kid->processTreeNode(kid));
		}
	}
}
