package tz.go.moh.him.dhis2.mediator.elmis;

import akka.actor.Props;
import org.openhim.mediator.engine.testing.MockLauncher;

import java.util.ArrayList;
import java.util.List;

public class CustomMockLauncher extends MockLauncher {

    public CustomMockLauncher(List<ActorToLaunch> actorsToLaunch) {
        super(actorsToLaunch);
    }

    public CustomMockLauncher(Class<Object> aClass, String expectedMessageType, String name) {
        super(new ArrayList<>());
        this.getContext().actorOf(Props.create(aClass, expectedMessageType), name);
    }

}