package ${package}.gitb;

import com.gitb.tr.ObjectFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class responsible for creating the object factory for GITB TDL types.
 */
@Configuration
public class ObjectFactoryConfig {

    /**
     * The ObjectFactory used to construct GITB classes.
     *
     * @return The factory.
     */
    @Bean
    public ObjectFactory objectFactory() {
        return new ObjectFactory();
    }

}
