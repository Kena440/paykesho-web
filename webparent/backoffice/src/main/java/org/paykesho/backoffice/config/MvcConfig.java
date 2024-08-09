package org.paykesho.backoffice.config;

import org.paykesho.backoffice.paging.PagingAndSortingArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

  /*  @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        exposeDirectory("user_photos", registry);
        exposeDirectory("../category_images", registry);
        exposeDirectory("../brand_images", registry);
        exposeDirectory("../product_images", registry);
        exposeDirectory("../store_logos", registry);
        exposeDirectory("../site_logo", registry);
    }

    private void exposeDirectory(String pathPattern, ResourceHandlerRegistry registry) {
        Path path = Paths.get(pathPattern);
        String absolutePath = path.toFile().getAbsolutePath();

        String logicalPath = pathPattern.replace("../", "") + "/**";

        registry.addResourceHandler(logicalPath)
                .addResourceLocations("file:/" + absolutePath + "/");
    }*/

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PagingAndSortingArgumentResolver());
    }
}
