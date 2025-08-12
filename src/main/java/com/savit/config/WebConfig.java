package com.savit.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;

@Configuration
@ComponentScan(basePackages = "com.savit")
public class WebConfig extends AbstractAnnotationConfigDispatcherServletInitializer {

    // 루트 설정 클래스 (DB, 보안 등 전역 설정)
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{
                RootConfig.class
        };
    }

    // 서블릿 관련 설정 클래스
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{
                SwaggerConfig.class,
                ServletConfig.class
        };
    }

    // DispatcherServlet URL 매핑
    @Override
    protected String[] getServletMappings() {
        return new String[]{ "/" };
    }

    // UTF-8 문자 인코딩 필터
    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        return new Filter[]{ encodingFilter };
    }

    // 404 예외 발생 설정
    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");
        String location = System.getProperty("java.io.tmpdir"); // 톰캣이 쓸 수 있는 경로
        long maxFileSize = 20L * 1024 * 1024;     // 20MB
        long maxRequestSize = 40L * 1024 * 1024;  // 40MB
        int fileSizeThreshold = 0;                // 즉시 디스크

        registration.setMultipartConfig(
                new MultipartConfigElement(location, maxFileSize, maxRequestSize, fileSizeThreshold)
        );
    }

    // CORS 설정 Bean (Vue 프론트 허용)
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization")
                        .allowCredentials(true);
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("swagger-ui.html")
                        .addResourceLocations("classpath:/META-INF/resources/");
                registry.addResourceHandler("/v2/api-docs")
                        .addResourceLocations("classpath:/META-INF/resources/");
                registry.addResourceHandler("/webjars/**")
                        .addResourceLocations("classpath:/META-INF/resources/webjars/");
            }

            @Override
            public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
                configurer.enable();
            }
        };
    }
}
