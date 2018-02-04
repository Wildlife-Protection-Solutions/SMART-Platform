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
package org.wcs.smart.connect.cybertracker;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.wcs.smart.connect.cybertracker.model.ConnectAlert;
import org.wcs.smart.dataentry.ICmItemListener;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;

/**
 * Configurable Model listener that is connected with correspondent DataModel listener
 * and is used for removing connect data associated with removed items.
 * 
 * @author elitvin
 * @since 6.0.0
 */
public class ConnectCtCmItemListener implements ICmItemListener {

	@Override
	public void deleteItem(Session currentSession, Object itemToDelete) throws Exception {
		if (itemToDelete instanceof CmAttributeListItem || itemToDelete instanceof CmAttributeTreeNode || itemToDelete instanceof CmNode) {
			deleteMatchingAlerItem(currentSession, itemToDelete);
		} else if (itemToDelete instanceof CmAttribute) {
			deleteMatchingCmAttribute(currentSession, (CmAttribute)itemToDelete);
		}
	}

	private void deleteMatchingAlerItem(Session currentSession, Object item) {
		CriteriaBuilder cb = currentSession.getCriteriaBuilder();
		CriteriaDelete<ConnectAlert> c = cb.createCriteriaDelete(ConnectAlert.class);
		Root<ConnectAlert> root = c.from(ConnectAlert.class);
		c.where(cb.equal(root.get("alertItem"), item)); //$NON-NLS-1$
		currentSession.createQuery(c).executeUpdate();
	}

	private void deleteMatchingCmAttribute(Session currentSession, CmAttribute cmAttr) {
		CriteriaBuilder cb = currentSession.getCriteriaBuilder();
		CriteriaDelete<ConnectAlert> c = cb.createCriteriaDelete(ConnectAlert.class);
		Root<ConnectAlert> root = c.from(ConnectAlert.class);
		c.where(cb.equal(root.get("attrubute"), cmAttr)); //$NON-NLS-1$
		currentSession.createQuery(c).executeUpdate();
	}

}