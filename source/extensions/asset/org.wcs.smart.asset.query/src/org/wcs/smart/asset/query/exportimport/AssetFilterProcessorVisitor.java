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
package org.wcs.smart.asset.query.exportimport;

import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.model.AssetQueryOptionType;
import org.wcs.smart.asset.query.parser.internal.filter.AssetFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;

/**
 * Filter visitor for processing asset filters.
 * 
 * @author Emily
 *
 */
public class AssetFilterProcessorVisitor implements IFilterVisitor {

	/**
     * Converts a asset option and associated uuid option to
     * a xml uuiditemtype
     *
     * @param option
     * @param uuid
     * @param session
     * @return
     * @throws Exception
     */
    public static UuidItemType processAssetOption(AssetFilterOption option, UUID uuid, Session session) throws Exception{

        if (option.getType() == AssetQueryOptionType.UUID){
//            //we need to add a uuid type
//            //find item in database
//            if (NamedKeyItem.class.isAssignableFrom(option.getSourceClass())){
//            	//match items based on key
//            	NamedKeyItem ki = (NamedKeyItem)option.getObject(session,  uuid);
//            	item.getValue().add(ki.getKeyId());
//            }else{
            	//TODO: implement me
//            	//match items based on name
//            	String[] data = option.getNames(session, UuidUtils.stringToUuid(uuid), Locale.getDefault());
//            	if (data != null){
//            		int index = 0;
//            		if (data.length > 1){
//            			item.setId(data[0]);
//            			index = 1;
//            		}
//            		for (;index < data.length; index++){
//            			item.getValue().add(data[index]);
//            		}
//            	}
//            }

//            return item;
        	//TODO:
        }
        return null;
    }
    
	private Session session;
	private Exception ex;
	private QueryType qt;
	public AssetFilterProcessorVisitor(Session session, QueryType qt){
		this.session = session;
		this.qt =qt;
	}
	
	@Override
	public void visit(IFilter filter) {
		if (filter instanceof AssetFilter){
			AssetFilter pf = (AssetFilter)filter;
			try{
				UuidItemType item = processAssetOption(pf.getAssetOption(), pf.getValue(), session);
				if (item != null){
					qt.getUuiditem().add(item);
				}
			}catch (Exception ex){
				this.ex = ex;
			}
		}
	}
	
	public Exception getException(){
		return this.ex;
	}
	
	

}
