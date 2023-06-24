/*
 * Copyright (C) 2022 Wildlife Conservation Society
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
package org.wcs.smart.i2;

import static org.hibernate.internal.CoreLogging.messageLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.query.ResultListTransformer;

public class DistinctResultListTransformer<T> implements ResultListTransformer<T>{

	private static final CoreMessageLogger LOG = messageLogger( DistinctResultListTransformer.class );

	/**
	 * Helper class to handle distincting
	 */
	private static final class Identity {
		final Object entity;

		private Identity(Object entity) {
			this.entity = entity;
		}

		@Override
		public boolean equals(Object other) {
			return Identity.class.isInstance( other )
					&& this.entity == ( (Identity) other ).entity;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode( entity );
		}
	}
	
	@Override
	public List<T> transformList(List<T> list) {	
		List<T> result = new ArrayList<T>( list.size() );
		Set<Identity> distinct = new HashSet<Identity>();
		for ( T entity : list ) {
			if ( distinct.add( new Identity( entity ) ) ) {
				result.add( entity );
			}
		}
		LOG.debugf( "Transformed: %s rows to: %s distinct results", list.size(), result.size() ); //$NON-NLS-1$
		return result;
	}

}
