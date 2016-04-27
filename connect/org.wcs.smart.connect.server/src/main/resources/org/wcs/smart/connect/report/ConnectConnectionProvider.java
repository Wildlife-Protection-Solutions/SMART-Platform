package org.wcs.smart.connect.report;

import java.util.Locale;

import javax.servlet.ServletContext;

import org.hibernate.Session;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.udig.catalog.smart.IDatabaseConnectionProvider;

public class ConnectConnectionProvider implements IDatabaseConnectionProvider {

	private static final long serialVersionUID = 1L;

	private ServletContext context;
	private Locale l;
	public ConnectConnectionProvider(ServletContext context, Locale l){
		this.context = context;
		this.l = l;
	}
	
	@Override
	public Session openSession() {
		return HibernateManager.openNewSession(context,l);
	}
	
	@Override
	public Locale getLocale() {
		return l;
	}

}
