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
package org.wcs.smart.connect.hibernate;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Optional;

/**
 * URL stream handler that wraps and existing factory.  It supports
 * custom processing for the "platform" protocol.  It was initially implemented
 * to support custom datamodel svg images on the map  
 * 
 * @author Emily
 *
 */
public class SMARTURLStreamHandlerProvider implements URLStreamHandlerFactory {

    // The wrapped URLStreamHandlerFactory's instance
    private final Optional<URLStreamHandlerFactory> delegate;

    /**
     * Used in case there is no existing URLStreamHandlerFactory defined
     */
    public SMARTURLStreamHandlerProvider() {
        this(null);
    }

    /**
     * Used in case there is an existing URLStreamHandlerFactory defined
     */
    public SMARTURLStreamHandlerProvider(final URLStreamHandlerFactory delegate) {
        this.delegate = Optional.ofNullable(delegate);
    }

    @Override
    public URLStreamHandler createURLStreamHandler(final String protocol) {
    	if (protocol.equalsIgnoreCase("platform")) { //$NON-NLS-1$
			return new PlatformUrlHandler();
		}
        // It is not the s3 protocol so we delegate it to the wrapped 
        // URLStreamHandlerFactory
        return delegate.map(factory -> factory.createURLStreamHandler(protocol))
            .orElse(null);
    }
}
