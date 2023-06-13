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
package org.wcs.smart.dataentry;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.datamodel.IDataModelListener;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Deals with removing disabled elements
 * from the configured models, once when the user
 * hits the save button.
 * 
 * @author Emily
 *
 */
public class DataModelListener implements IDataModelListener {

	@Override
	public void modified() {
		if (SmartDB.isMultipleAnalysis()){
			return;
		}
		
		try(Session session = HibernateManager.openSession()){
		
			session.beginTransaction();
			try{
				//delete all disabled list items
				session.createMutationQuery("DELETE CmAttributeListItem a where a IN (SELECT cma FROM CmAttributeListItem cma WHERE cma.listItem.attribute.conservationArea = :ca and cma.listItem.isActive = 'false')") //$NON-NLS-1$
					.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.executeUpdate();
				session.flush();
				
				//delete default attribute option
				session.createMutationQuery("DELETE CmAttributeOption op WHERE op IN (SELECT cop FROM CmAttributeOption cop, AttributeListItem it WHERE cop.optionId = '" + CmAttributeOption.ID_DEFAULT_VALUE + "' AND cop.uuidValue = it.uuid and it.isActive = 'false' and it.attribute.conservationArea = :ca)") //$NON-NLS-1$ //$NON-NLS-2$
					.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.executeUpdate();
				session.flush();
				
				//delete all disabled tree items
				session.createMutationQuery("DELETE CmAttributeTreeNode a WHERE a IN (SELECT cm FROM CmAttributeTreeNode cm WHERE cm.dmTreeNode.attribute.conservationArea = :ca AND cm.dmTreeNode.isActive = 'false')") //$NON-NLS-1$
					.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.executeUpdate();
				session.flush();
				
				//delete default attribute option
				session.createMutationQuery("DELETE CmAttributeOption op WHERE op IN (SELECT cop FROM CmAttributeOption cop, AttributeTreeNode it WHERE cop.optionId = '" + CmAttributeOption.ID_DEFAULT_VALUE + "' AND cop.uuidValue = it.uuid and it.isActive = 'false' and it.attribute.conservationArea = :ca)") //$NON-NLS-1$ //$NON-NLS-2$
					.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.executeUpdate();
				session.flush();
				
				//delete all nodes that reference in-active categories
				Query<CmNode> nodeQuery = session.createQuery("FROM CmNode n WHERE n.category.conservationArea = :ca AND n.category.isActive = 'false'", CmNode.class); //$NON-NLS-1$
				nodeQuery.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
				List<CmNode> toDelete = nodeQuery.list();
				for (CmNode delete : toDelete){
					//this seems to be required for hibernate to correctly remove category
					//see: https://app.assembla.com/spaces/smart-cs/tickets/3433
					delete.getCmAttributes().clear();
					
					if (delete.getParent() != null){
						//remove from parent are re-order siblings
						delete.getParent().getChildren().remove(delete);
						int i = 0;
						for (CmNode sibling : delete.getParent().getChildren()){
							sibling.setNodeOrder(i++);
						}
						delete.setParent(null);
					}else{
						//part of root
						session.remove(delete);
						//TODO: re-order root nodes
					}
					session.flush();
				}
				session.flush();
				
				//delete all attributes that reference invalid attributes
				//note it could be parent category attribute
				Query<CmAttribute> qAttribute = session.createQuery("SELECT a FROM CmAttribute a, CategoryAttribute b " + //$NON-NLS-1$
						"WHERE a.attribute = b.id.attribute " + //$NON-NLS-1$
						"AND a.attribute.conservationArea = b.id.attribute.conservationArea AND a.attribute.conservationArea = :ca " + //$NON-NLS-1$
						"AND a.node.category.hkey like concat(b.id.category.hkey, '%') " + //$NON-NLS-1$
						"and b.isActive = 'false'", CmAttribute.class); //$NON-NLS-1$
				qAttribute.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
				List<CmAttribute> toDelete2 =  qAttribute.list();
				for(CmAttribute delete : toDelete2){
					delete.getNode().getCmAttributes().remove(delete);
					int i =0;
					for (CmAttribute silbing : delete.getNode().getCmAttributes()){
						silbing.setOrder(i++);
					}
					delete.setNode(null);
				}
				session.flush();
				session.getTransaction().commit();
			}catch (Exception ex){
				if(session.getTransaction().isActive()){
					session.getTransaction().rollback();
				}
				throw ex;
			}
		}
	}

}
