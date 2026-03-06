package ro.simavi.sphinx.ad.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;

public class RestCallErrorHandler extends DefaultResponseErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestCallErrorHandler.class);

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        LOGGER.error(response.getStatusCode().getReasonPhrase());
        LOGGER.error(new String(this.getResponseBody(response)));
        super.handleError(response);
    }
}
