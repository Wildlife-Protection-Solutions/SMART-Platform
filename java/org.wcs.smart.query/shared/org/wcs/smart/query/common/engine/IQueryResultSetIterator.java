package org.wcs.smart.query.common.engine;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;


public interface IQueryResultSetIterator<T extends IResultItem> extends Iterator<IResultItem>, Closeable {

	@Override
	public boolean hasNext();
	
	@Override
	public IResultItem next();
	
	@Override
	public void remove();
	
	@Override
	public void close() throws IOException;
	
}
