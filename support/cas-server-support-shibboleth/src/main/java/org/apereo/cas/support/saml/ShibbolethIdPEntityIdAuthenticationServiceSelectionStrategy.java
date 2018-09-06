package org.apereo.cas.support.saml;

import com.google.common.base.Splitter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apereo.cas.authentication.AuthenticationServiceSelectionStrategy;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceAccessStrategyUtils;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.web.support.WebUtils;
import org.springframework.core.Ordered;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * This is {@link ShibbolethIdPEntityIdAuthenticationServiceSelectionStrategy}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Slf4j
@AllArgsConstructor
public class ShibbolethIdPEntityIdAuthenticationServiceSelectionStrategy implements AuthenticationServiceSelectionStrategy {
    private static final long serialVersionUID = -2059445756475980894L;

    private final int order = Ordered.HIGHEST_PRECEDENCE;
    private final transient ServiceFactory webApplicationServiceFactory;
    private final String idpServerPrefix;
    private final ServicesManager servicesManager;

    private static final String PARAMETER_ENTITY_ID = "entityId";


    @Override
    public Service resolveServiceFrom(final Service service) {
        final Optional<String> result = getEntityIdAsParameter(service);
        if (result.isPresent()) {
            final String entityId = result.get();
            LOGGER.debug("Located entity id [{}] from service authentication request at [{}]", entityId, service.getId());
            final RegisteredService registeredService = servicesManager.findServiceBy(entityId);
            if (registeredService != null && registeredService.getAccessStrategy() !=  null
                && registeredService.getAccessStrategy().isServiceAccessAllowed()) {
                return this.webApplicationServiceFactory.createService(entityId);
            }
            LOGGER.debug("Entity id [{}] not registered as individual service", entityId);
        }
        LOGGER.debug("Could not located entity id from service authentication request at [{}]", service.getId());
        return service;
    }

    @Override
    public boolean supports(final Service service) {
        final String casPattern = "^".concat(idpServerPrefix).concat(".*");
        return service != null && service.getId().matches(casPattern)
                && getEntityIdAsParameter(service).isPresent();
    }

    /**
     * Gets entity id as parameter.
     *
     * @param service the service
     * @return the entity id as parameter
     */
    protected static Optional<String> getEntityIdAsParameter(final Service service) {
        try {
            final URIBuilder builder = new URIBuilder(service.getId());
            final Optional<NameValuePair> param = builder.getQueryParams()
                    .stream()
                    .filter(p -> p.getName().equals(PARAMETER_ENTITY_ID))
                    .findFirst();

            if (param.isPresent()) {
                LOGGER.debug("Found Entity Id in Service id = " + param.get().getValue());
                return Optional.of(param.get().getValue());
            }
            final HttpServletRequest request = WebUtils.getHttpServletRequestFromExternalWebflowContext();
            if (request != null && StringUtils.isNotBlank(request.getQueryString())) {
                final String[] query = request.getQueryString().split("&");
                final Optional<String> paramRequest = Arrays.stream(query)
                        .map(p -> {
                            final List<String> params = Splitter.on("=").splitToList(p);
                            return Pair.of(params.get(0), params.get(1));
                        })
                        .filter(p -> p.getKey().equals(PARAMETER_ENTITY_ID))
                        .map(Pair::getValue)
                        .map(ShibbolethIdPEntityIdAuthenticationServiceSelectionStrategy::decode)
                        .findFirst();
                LOGGER.debug("Found entity id as part of request url = " + paramRequest);
                return paramRequest;
            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    protected static String decode(final String entityId) {
        try {
            return URLDecoder.decode(entityId, "UTF-8");
        } catch (final Exception e) {
            LOGGER.error("Error occurred try to decode entity id");
            return entityId;
        }
    }
}
