package org.paykesho.clientportal.config;

import org.paykesho.clientportal.paging.PagingAndSortingArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

/*    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        exposeDirectory("../category_images", registry);
        exposeDirectory("../brand_images", registry);
        exposeDirectory("../product_images", registry);
        exposeDirectory("../store_logos", registry);
    }

    private void exposeDirectory(String pathPattern, ResourceHandlerRegistry registry) {
        Path path = Paths.get(pathPattern);
        String absolutePath = path.toFile().getAbsolutePath();

        String logicalPath = pathPattern.replace("../", "") + "/**";

        registry.addResourceHandler(logicalPath)
                .addResourceLocations("file:/" + absolutePath + "/");
    }*/

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PagingAndSortingArgumentResolver());
    }
}
