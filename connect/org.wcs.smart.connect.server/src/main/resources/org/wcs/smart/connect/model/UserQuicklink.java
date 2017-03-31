/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.model;


import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

/**
 * A UserQuicklink entity. Keeps a link specific to a single user, all of these are shown on their homepage. "order" is the order users want the links (Ascending) displayed.  
 *
 * @Author Jeff
 */
@Entity
@Table(name = "connect.user_quicklinks")
public class UserQuicklink extends ConnectUuidItem{
	private UUID userUuid; 
	private Quicklink quicklink;
	private String labelOverride; 
	private int order;
	
    @JoinColumn(name = "quicklink_uuid")
    @Type(type = "pg-uuid")
    @ManyToOne
	public Quicklink getQuicklink() {
		return quicklink;
	}
	public void setQuicklink(Quicklink quicklink) {
		this.quicklink = quicklink;
	}
	
	
	@Column(name="user_uuid")
	public UUID getUserUuid() {
		return userUuid;
	}
	public void setUserUuid(UUID userUuid) {
		this.userUuid = userUuid;
	}
	
	@Column(name="label_override")
	public String getLabelOverride() {
		return labelOverride;
	}
	public void setLabelOverride(String labelOverride) {
		this.labelOverride = labelOverride;
	}
	
	@Column(name="link_order")
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}
		
}
