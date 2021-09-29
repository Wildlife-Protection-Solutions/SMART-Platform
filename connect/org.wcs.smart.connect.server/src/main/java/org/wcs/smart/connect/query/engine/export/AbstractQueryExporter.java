package org.wcs.smart.connect.query.engine.export;

import java.sql.SQLException;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.ws.rs.core.StreamingOutput;

import org.hibernate.Session;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.query.common.engine.IQueryResult;

public abstract class AbstractQueryExporter implements StreamingOutput {

	protected Locale locale;
	protected ServletContext context;
	protected IProjectionProvider prjProvider;
	
	public AbstractQueryExporter(IProjectionProvider prjProvider, Locale locale, ServletContext context) {
		this.locale = locale;
		this.context = context;
		this.prjProvider = prjProvider;
	}
	
	
	
	protected void dispose(IQueryResult result, Session session) throws SQLException {
		result.dispose(session);
	}

	protected void dispose(org.wcs.smart.i2.query.IQueryResult result, Session session) throws SQLException {
		result.dispose(session);
	}

	
}
