package tz.go.moh.him.dhis2.mediator;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.openhim.mediator.engine.MediatorConfig;
import org.openhim.mediator.engine.messages.FinishRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPResponse;
import tz.go.moh.him.dhis2.mediator.domain.DHIS2Response;
import tz.go.moh.him.mediator.core.serialization.JsonSerializer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Dhis2Orchestrator extends UntypedActor {
    /**
     * The serializer.
     */
    protected static final JsonSerializer serializer = new JsonSerializer();

    /**
     * The mediator configuration.
     */
    private final MediatorConfig config;
    /**
     * The logger instance.
     */
    private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    /**
     * The request handler that handles requests and responses.
     */
    private ActorRef requestHandler;

    /**
     * Initializes a new instance of the {@link Dhis2Orchestrator} class.
     *
     * @param config The mediator configuration.
     */
    public Dhis2Orchestrator(MediatorConfig config) {
        this.config = config;
    }

    /**
     * Forwards the message to the Tanzania Supply Chain Portal
     *
     * @param message to be sent to the Thscp
     */
    private void forwardToDhis2(String message) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        String scheme;
        String host;
        String path;
        String username;
        String password;
        int portNumber;
        if (config.getDynamicConfig().isEmpty()) {
            log.debug("Dynamic config is empty, using config from mediator.properties");
            if (config.getProperty("destination.scheme").equals("https")) {
                scheme = "https";
            } else {
                scheme = "http";
            }

            host = config.getProperty("destination.host");
            portNumber = Integer.parseInt(config.getProperty("destination.api.port"));
            path = config.getProperty("destination.api.path");

        } else {
            log.debug("Using dynamic config");

            JSONObject connectionProperties = new JSONObject(config.getDynamicConfig()).getJSONObject("destinationConnectionProperties");

            host = connectionProperties.getString("destinationHost");
            portNumber = connectionProperties.getInt("destinationPort");
            scheme = connectionProperties.getString("destinationScheme");

            if (connectionProperties.has("destinationUsername") && connectionProperties.has("destinationPassword")) {
                username = connectionProperties.getString("destinationUsername");
                password = connectionProperties.getString("destinationPassword");

                // if we have a username and a password
                // we want to add the username and password as the Basic Auth header in the HTTP request
                if (username != null && !"".equals(username) && password != null && !"".equals(password)) {
                    String auth = username + ":" + password;
                    byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
                    String authHeader = "Basic " + new String(encodedAuth);
                    headers.put(HttpHeaders.AUTHORIZATION, authHeader);
                }
            }

            path = connectionProperties.getString("destinationPath");
        }
        List<Pair<String, String>> params = new ArrayList<>();

        host = scheme + "://" + host + ":" + portNumber + path;

        MediatorHTTPRequest forwardToDhis2Request = new MediatorHTTPRequest(
                requestHandler, getSelf(), "Sending Data to DHIS2", "POST",
                host, message, headers, params
        );

        ActorSelection httpConnector = getContext().actorSelection(config.userPathFor("http-connector"));
        httpConnector.tell(forwardToDhis2Request, getSelf());
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof MediatorHTTPRequest) {
            log.info("Sending data DHIS2 ...");
            requestHandler = ((MediatorHTTPRequest) msg).getRequestHandler();
            forwardToDhis2(((MediatorHTTPRequest) msg).getBody());
        } else if (msg instanceof MediatorHTTPResponse) { //respond
            log.info("Received response from DHIS2");

            DHIS2Response dhis2Response = serializer.deserialize(((MediatorHTTPResponse) msg).getBody(), DHIS2Response.class);

            if (((MediatorHTTPResponse) msg).getStatusCode() == HttpStatus.SC_OK && !dhis2Response.getStatus().equalsIgnoreCase("success") && !dhis2Response.getStatus().equalsIgnoreCase("ok")) {
                FinishRequest responseFinishRequest = ((MediatorHTTPResponse) msg).toFinishRequest();
                requestHandler.tell(new FinishRequest(responseFinishRequest.getResponse(), responseFinishRequest.getResponseHeaders(), HttpStatus.SC_BAD_REQUEST), getSelf());
            } else {
                requestHandler.tell(((MediatorHTTPResponse) msg).toFinishRequest(), getSelf());
            }
        } else {
            unhandled(msg);
        }
    }
}
