/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.i2.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.security.IntelSecurityManager;

import com.ibm.icu.text.MessageFormat;

import jakarta.persistence.Tuple;

/**
 * Searches record attribute; used for searching for duplicate records
 * 
 * @author Emily
 *
 */
public class AttributeRecordSearch extends BasicRecordSearch {

	protected IntelRecordAttributeValue attributeValue;
	
	
	public AttributeRecordSearch(IntelRecordSource source, IntelRecordAttributeValue attributeValue) {
		super(source, null,null);
		this.attributeValue = attributeValue;
	}

	
	@Override
	public IntelRecordResult doSearch(Session session, IProgressMonitor monitor) {
		Long startTime = System.nanoTime();
		
		//IntelRecordAttributeValue
		String hql = " FROM IntelRecord r join r.attributes a WHERE r.profile in (:profiles) "; //$NON-NLS-1$
		
		hql += " AND a.attribute.attribute = :attribute "; //$NON-NLS-1$
		
		if (attributeValue.getAttribute().getAttribute().getType() == AttributeType.TEXT) {
			hql += " AND a.stringValue = :value1 "; //$NON-NLS-1$
		}else if (attributeValue.getAttribute().getAttribute().getType() == AttributeType.NUMERIC) {
			hql += " AND a.numberValue = :value1 "; //$NON-NLS-1$
		}else if (attributeValue.getAttribute().getAttribute().getType() == AttributeType.DATE) {
			hql += " AND a.stringValue = :value1 "; //$NON-NLS-1$
		}else {
			throw new IllegalStateException(MessageFormat.format("The attribute type {0} not supported for attribute searches", attributeValue.getAttribute().getAttribute().getType().getGuiName(Locale.getDefault()))); //$NON-NLS-1$
		}
				
		if (source != null){
			hql += " AND r.recordSource = :source "; //$NON-NLS-1$
		}
		if (narrativeSearch != null){
			hql += " AND lower(r.description) like lower(:narrative) "; //$NON-NLS-1$
		}
		if (titleSearch != null && titleLike){
			hql += " AND lower(r.title) like lower(:title) "; //$NON-NLS-1$
		}else if (titleSearch != null) {
			hql += " AND lower(r.title) = lower(:title) "; //$NON-NLS-1$
		}
		
		
		List<IntelProfile> profiles = new ArrayList<>(ProfilesManager.INSTANCE.getActiveProfiles());
		profiles = profiles.stream().filter(e->IntelSecurityManager.INSTANCE.canViewRecords(e)).collect(Collectors.toList());
		if (profiles.isEmpty()) {
			return new IntelRecordResult(0, Collections.emptyList(), System.nanoTime() - startTime);
		}
		Query<Long> query = session.createQuery("SELECT count(*) " + hql, Long.class); //$NON-NLS-1$
		query.setParameter("profiles", profiles); //$NON-NLS-1$
		query.setParameter("attribute", attributeValue.getAttribute().getAttribute()); //$NON-NLS-1$
		if (source != null){
			query.setParameter("source", source); //$NON-NLS-1$
		}
		if (narrativeSearch != null){
			query.setParameter("narrative", "%" + narrativeSearch + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (titleSearch != null){
			if (titleLike) {
				query.setParameter("title", "%" + titleSearch + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}else {
				query.setParameter("title", titleSearch); //$NON-NLS-1$ 
			}
		}
		if (attributeValue.getAttribute().getAttribute().getType() == AttributeType.TEXT) {
			query.setParameter("value1", attributeValue.getStringValue()); //$NON-NLS-1$
		}else if (attributeValue.getAttribute().getAttribute().getType() == AttributeType.NUMERIC) {
			query.setParameter("value1", attributeValue.getNumberValue()); //$NON-NLS-1$
		}else if (attributeValue.getAttribute().getAttribute().getType() == AttributeType.DATE) {
			query.setParameter("value1", attributeValue.getStringValue()); //$NON-NLS-1$
		}else {
			throw new IllegalStateException(MessageFormat.format("The attribute type {0} not supported for attribute searches", attributeValue.getAttribute().getAttribute().getType().getGuiName(Locale.getDefault()))); //$NON-NLS-1$
		}
		
		long cnt = query.uniqueResult();
		
		Query<Tuple> query2 = session.createQuery("SELECT r.uuid, r.title, r.recordSource.uuid, r.status, r.description, r.profile.uuid " + hql, Tuple.class); //$NON-NLS-1$
		query2.setMaxResults(IRecordSearch.MAX_RESULT_CNT);
		query2.setParameter("profiles", profiles); //$NON-NLS-1$
		query2.setParameter("attribute", attributeValue.getAttribute().getAttribute()); //$NON-NLS-1$
		if (source != null){
			query2.setParameter("source", source); //$NON-NLS-1$
		}
		if (narrativeSearch != null){
			query2.setParameter("narrative", "%" + narrativeSearch + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (titleSearch != null){
			if (titleLike) {
				query2.setParameter("title", "%" + titleSearch + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}else {
				query2.setParameter("title", titleSearch); //$NON-NLS-1$ 
			}
		}
		if (attributeValue.getAttribute().getAttribute().getType() == AttributeType.TEXT) {
			query2.setParameter("value1", attributeValue.getStringValue()); //$NON-NLS-1$
		}else if (attributeValue.getAttribute().getAttribute().getType() == AttributeType.NUMERIC) {
			query2.setParameter("value1", attributeValue.getNumberValue()); //$NON-NLS-1$
		}else if (attributeValue.getAttribute().getAttribute().getType() == AttributeType.DATE) {
			query2.setParameter("value1", attributeValue.getStringValue()); //$NON-NLS-1$
		}else {
			throw new IllegalStateException(MessageFormat.format("The attribute type {0} not supported for attribute searches", attributeValue.getAttribute().getAttribute().getType().getGuiName(Locale.getDefault()))); //$NON-NLS-1$
		}
		List<Tuple> items = query2.list();
		List<IntelRecordSearchResultItem> resultItems = new ArrayList<>();
				
		for (Tuple it : items){
			UUID uuid = it.get(0, UUID.class);
			String title = it.get(1, String.class);
			UUID src = it.get(2, UUID.class);
			IntelRecord.Status status = it.get(3, IntelRecord.Status.class);
			IntelProfile profile = session.get(IntelProfile.class, it.get(5, UUID.class));
			resultItems.add(new IntelRecordSearchResultItem(uuid, profile, src == null ? null : session.get(IntelRecordSource.class, src), title, status));
		}
		
		IntelRecordResult results = new IntelRecordResult(cnt, resultItems, System.nanoTime() - startTime);
		return results;
	}
}
