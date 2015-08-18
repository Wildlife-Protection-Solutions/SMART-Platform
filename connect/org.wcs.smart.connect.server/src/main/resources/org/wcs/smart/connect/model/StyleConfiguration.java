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
	private byte[] usersImage;
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
	@Column(name="users_image")
	public byte[] getUsersImage(){
		return this.usersImage;
	}
	public void setUsersImage(byte[] image){
		this.usersImage = image;
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
