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
package org.wcs.smart.ca;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Interceptor;
import org.wcs.smart.SmartPlugIn;

/**
 * Class that encapsulates multiple number of {@link Interceptor} objects
 * and allow them all to be called for one session (e.g. act act single {@link Interceptor}).
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class MultiInterceptor implements IInterceptorContainer, InvocationHandler {
	
	private List<Interceptor> interceptors;

    public static IMultiInterceptor createInstance() {
        return (IMultiInterceptor) Proxy.newProxyInstance(IMultiInterceptor.class.getClassLoader(), new Class[] { IMultiInterceptor.class }, new MultiInterceptor());
    }

	private MultiInterceptor() {
		this.interceptors = new ArrayList<>();
	}
	
	public MultiInterceptor(List<Interceptor> interceptors) {
	}

	public boolean add(Interceptor interceptor) {
		return this.interceptors.add(interceptor);
	}
	
	public boolean addAll(Collection<? extends Interceptor> c) {
		return this.interceptors.addAll(c);
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method m = findMethod(this.getClass(), method);
        if (m != null) {
            return m.invoke(this, args);
        }
        List<Object> results = new ArrayList<>(interceptors.size());
        for (Interceptor i : interceptors) {
            m = findMethod(i.getClass(), method);
            if (m != null) {
                Object result = m.invoke(i, args);
                results.add(result);
            } else {
            	SmartPlugIn.displayLog(MessageFormat.format("Failed to invoke interceptor method {0}. Some data may be lost.", method.getName()), null); //$NON-NLS-1$
            }
		}
        return !results.isEmpty() ? results.get(0) : null;
	}

    private Method findMethod(Class<?> clazz, Method method) throws Throwable {
        try {
            return clazz.getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
	
}
