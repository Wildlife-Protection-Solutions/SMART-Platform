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


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "connect.style_configuration")
public class StyleConfiguration extends ConnectUuidItem {

	private String styleId;
	private boolean active;
	private byte[] headerImage;
	private byte[] backgroundImage;
	private byte[] loginImage;
	private String serverName;
	private String footerText;
	
	
	@Column(name="style_id")
	public String getStyleId() {
		return styleId;
	}
	public void setStyleId(String style_id) {
		this.styleId = style_id;
	}
	
	@Column(name="active")
	public boolean getActive(){
		return this.active;
	}
	public void setActive(boolean active){
		this.active = active;
	}

	
	@Column(name="header_image")
	public byte[] getHeaderImage(){
		return this.headerImage;
	}
	public void setHeaderImage(byte[] image){
		this.headerImage = image;
	}
	
	@Column(name="background_image")
	public byte[] getBackgroundImage(){
		return this.backgroundImage;
	}
	public void setBackgroundImage(byte[] image){
		this.backgroundImage = image;
	}
	
	@Column(name="login_image")
	public byte[] getLoginImage(){
		return this.loginImage;
	}
	public void setLoginImage(byte[] image){
		this.loginImage = image;
	}
	
	@Column(name="server_name")
	public String getServerName(){
		return this.serverName;
	}
	public void setServerName(String name){
		this.serverName = name;
	}
	
	@Column(name="footer_text")
	public String getFooterText(){
		return this.footerText;
	}
	public void setFooterText(String text){
		this.footerText= text;
	}
}
