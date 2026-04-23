package com.facesend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        List<MediaType> types = new ArrayList<>();
        types.add(MediaType.APPLICATION_JSON);
        types.add(MediaType.TEXT_PLAIN);
        types.add(MediaType.TEXT_HTML);
        types.add(MediaType.APPLICATION_FORM_URLENCODED);
        types.add(MediaType.MULTIPART_FORM_DATA);
        converter.setSupportedMediaTypes(types);
        converters.add(0, converter);
    }
}
