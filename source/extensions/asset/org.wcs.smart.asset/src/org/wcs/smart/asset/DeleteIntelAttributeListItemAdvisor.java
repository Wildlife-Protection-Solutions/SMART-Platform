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
package org.wcs.smart.asset;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.asset.model.AssetAttributeValue;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetStationAttributeValue;
import org.wcs.smart.ca.advisors.IDeleteAdvisor;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * Validates the deletion of attribute list items
 * @author Emily
 *
 */
public class DeleteIntelAttributeListItemAdvisor implements IDeleteAdvisor {

	public DeleteIntelAttributeListItemAdvisor() {
		// TODO Auto-generated constructor stub
	}


	@Override
	public String canDelete(Object object, Session session) {
		if (!(object instanceof AssetAttributeListItem)){
			return "Object not an AssetAttributeListItem object.  Cannot delete.";
		}
		AssetAttributeListItem attribute = (AssetAttributeListItem) object;
		
		Long linkCnt =  QueryFactory.buildCountQuery(session, AssetAttributeValue.class, new Object[] {"attributeListItem", attribute}); //$NON-NLS-1$
		if (linkCnt > 0){
			return MessageFormat.format("This attribute list item is referenced by {0} asset attributes.  These references must be removed before you can delete this item.", linkCnt);
		}
		
		linkCnt =  QueryFactory.buildCountQuery(session, AssetStationAttributeValue.class, new Object[] {"attributeListItem", attribute}); //$NON-NLS-1$
		if (linkCnt > 0){
			return MessageFormat.format("This attribute list item is referenced by {0} station attributes.  These references must be removed before you can delete this item.", linkCnt);
		}
		
		linkCnt =  QueryFactory.buildCountQuery(session, AssetDeploymentAttributeValue.class, new Object[] {"attributeListItem", attribute}); //$NON-NLS-1$
		if (linkCnt > 0){
			return MessageFormat.format("This attribute list item is referenced by {0} asset deployment attributes.  These references must be removed before you can delete this item.", linkCnt);
		}
		
		return null;
	}

}
