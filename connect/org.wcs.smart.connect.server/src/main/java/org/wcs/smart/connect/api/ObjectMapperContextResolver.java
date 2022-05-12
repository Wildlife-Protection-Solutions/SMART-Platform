package org.wcs.smart.connect.api;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Customized ObjectMapper to register jsr310 datetime serializers 
 * @author Emily
 *
 */
@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper>{

    private final ObjectMapper mapper;
    
    public ObjectMapperContextResolver() {
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }

}
