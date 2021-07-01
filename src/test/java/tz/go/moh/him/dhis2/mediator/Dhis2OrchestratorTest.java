package tz.go.moh.him.dhis2.mediator;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openhim.mediator.engine.MediatorConfig;
import org.openhim.mediator.engine.messages.FinishRequest;
import org.openhim.mediator.engine.messages.MediatorHTTPRequest;
import org.openhim.mediator.engine.testing.TestingUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class Dhis2OrchestratorTest {

    /**
     * Represents the configuration.
     */
    protected static MediatorConfig configuration;

    /**
     * Represents the system actor.
     */
    protected static ActorSystem system;

    /**
     * Runs cleanup after class execution.
     */
    @AfterClass
    public static void afterClass() {
        TestingUtils.clearRootContext(system, configuration.getName());
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    /**
     * Runs initialization before each class execution.
     */
    @BeforeClass
    public static void beforeClass() throws IOException {
        configuration = loadConfig(null);
        system = ActorSystem.create();
    }

    /**
     * Loads the mediator configuration.
     *
     * @param configPath The configuration path.
     * @return Returns the mediator configuration.
     */
    public static MediatorConfig loadConfig(String configPath) throws IOException {
        MediatorConfig config = new MediatorConfig();

        if (configPath != null) {
            Properties props = new Properties();
            File conf = new File(configPath);
            InputStream in = FileUtils.openInputStream(conf);
            props.load(in);
            IOUtils.closeQuietly(in);

            config.setProperties(props);
        } else {
            config.setProperties("mediator.properties");
        }

        config.setName(config.getProperty("mediator.name"));
        config.setServerHost(config.getProperty("mediator.host"));
        config.setServerPort(Integer.parseInt(config.getProperty("mediator.port")));
        config.setRootTimeout(Integer.parseInt(config.getProperty("mediator.timeout")));

        config.setCoreHost(config.getProperty("core.host"));
        config.setCoreAPIUsername(config.getProperty("core.api.user"));
        config.setCoreAPIPassword(config.getProperty("core.api.password"));

        config.setCoreAPIPort(Integer.parseInt(config.getProperty("core.api.port")));
        config.setHeartbeatsEnabled(true);

        return config;
    }

    /**
     * Runs cleanup after each test execution.
     */
    @After
    public void after() {
        system = ActorSystem.create();
    }

    @Test
    public void testDhis2WarningResponsesMediatorHTTPRequest() throws Exception {
        assertNotNull(system);
        system.actorOf(Props.create(CustomMockLauncher.class, MockDestination.class, "WARNING", "http-connector"), configuration.getName());
        new JavaTestKit(system) {{
            InputStream stream = Dhis2OrchestratorTest.class.getClassLoader().getResourceAsStream("elmis_request.json");

            final ActorRef defaultOrchestrator = system.actorOf(Props.create(Dhis2Orchestrator.class, configuration));

            MediatorHTTPRequest POST_Request = new MediatorHTTPRequest(
                    getRef(),
                    getRef(),
                    "unit-test",
                    "POST",
                    "http",
                    null,
                    null,
                    "/dhis2",
                    IOUtils.toString(stream),
                    Collections.<String, String>singletonMap("Content-Type", "text/plain"),
                    Collections.<Pair<String, String>>emptyList()
            );

            defaultOrchestrator.tell(POST_Request, getRef());

            final Object[] out =
                    new ReceiveWhile<Object>(Object.class, duration("1 second")) {
                        @Override
                        protected Object match(Object msg) throws Exception {
                            if (msg instanceof FinishRequest) {
                                return msg;
                            }
                            throw noMatch();
                        }
                    }.get();

            assertTrue(Arrays.stream(out).anyMatch(c -> c instanceof FinishRequest));
            assertTrue(Arrays.stream(out).allMatch(c -> (c instanceof FinishRequest) && ((FinishRequest) c).getResponseStatus() == HttpStatus.SC_BAD_REQUEST));
        }};
    }

    @Test
    public void testDhis2SuccessResponsesMediatorHTTPRequest() throws Exception {
        assertNotNull(system);
        system.actorOf(Props.create(CustomMockLauncher.class, MockDestination.class, "SUCCESS", "http-connector"), configuration.getName());
        new JavaTestKit(system) {{
            InputStream stream = Dhis2OrchestratorTest.class.getClassLoader().getResourceAsStream("elmis_request.json");

            final ActorRef defaultOrchestrator = system.actorOf(Props.create(Dhis2Orchestrator.class, configuration));

            MediatorHTTPRequest POST_Request = new MediatorHTTPRequest(
                    getRef(),
                    getRef(),
                    "unit-test",
                    "POST",
                    "http",
                    null,
                    null,
                    "/dhis2",
                    IOUtils.toString(stream),
                    Collections.<String, String>singletonMap("Content-Type", "text/plain"),
                    Collections.<Pair<String, String>>emptyList()
            );

            defaultOrchestrator.tell(POST_Request, getRef());

            final Object[] out =
                    new ReceiveWhile<Object>(Object.class, duration("1 second")) {
                        @Override
                        protected Object match(Object msg) throws Exception {
                            if (msg instanceof FinishRequest) {
                                return msg;
                            }
                            throw noMatch();
                        }
                    }.get();

            assertTrue(Arrays.stream(out).anyMatch(c -> c instanceof FinishRequest));
            assertTrue(Arrays.stream(out).allMatch(c -> (c instanceof FinishRequest) && ((FinishRequest) c).getResponseStatus() == HttpStatus.SC_OK));
        }};
    }
}
