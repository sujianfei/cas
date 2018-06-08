package org.apereo.cas.support.saml.services.idp.metadata.cache.resolver;

import org.apereo.cas.category.FileSystemCategory;
import org.apereo.cas.config.CasCoreHttpConfiguration;
import org.apereo.cas.config.CoreSamlConfiguration;
import org.apereo.cas.configuration.model.support.saml.idp.SamlIdPProperties;
import org.apereo.cas.support.saml.OpenSamlConfigBean;
import org.apereo.cas.support.saml.services.SamlRegisteredService;
import org.apereo.cas.util.http.SimpleHttpClient;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import lombok.extern.slf4j.Slf4j;

import static org.junit.Assert.*;

/**
 * This is {@link DynamicResourceMetadataResolverTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@RunWith(SpringRunner.class)
@Slf4j
@SpringBootTest(classes = {
    RefreshAutoConfiguration.class,
    CasCoreHttpConfiguration.class,
    CoreSamlConfiguration.class
})
@Category(FileSystemCategory.class)
public class DynamicResourceMetadataResolverTests {

    @Autowired
    @Qualifier("httpClient")
    private SimpleHttpClient httpClient;

    @Autowired
    @Qualifier("shibboleth.OpenSAMLConfig")
    private OpenSamlConfigBean openSamlConfigBean;

    @Test
    public void verifyResolverSupports() {
        final SamlIdPProperties props = new SamlIdPProperties();
        final DynamicMetadataResolver resolver = new DynamicMetadataResolver(props, openSamlConfigBean);
        final SamlRegisteredService service = new SamlRegisteredService();
        service.setMetadataLocation("http://www.testshib.org/metadata/testshib-providers.xml");
        assertFalse(resolver.supports(service));
        service.setMetadataLocation("http://mdq-beta.incommon.org/global/entities/{0}");
        assertTrue(resolver.supports(service));
    }

    @Test
    public void verifyResolverResolves() {
        final SamlIdPProperties props = new SamlIdPProperties();
        final DynamicMetadataResolver resolver = new DynamicMetadataResolver(props, openSamlConfigBean);
        final SamlRegisteredService service = new SamlRegisteredService();
        service.setId(100);
        service.setName("Dynamic");
        service.setMetadataLocation("http://mdq-beta.incommon.org/global/entities/{0}");
        service.setServiceId("https://webauth.cmc.edu/idp/shibboleth");
        final Collection results = resolver.resolve(service);
        assertFalse(results.isEmpty());
    }
}
