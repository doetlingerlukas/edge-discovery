package edge.discovery.graph;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import at.uibk.dps.ee.enactables.FactoryInputUser;
import at.uibk.dps.ee.enactables.serverless.FunctionFactoryServerless;
import at.uibk.dps.ee.guice.starter.VertxProvider;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;

import static org.mockito.Mockito.mock;
import java.util.HashSet;

class PsMappingLocalResTest {

  @Test
  void test() {
    String url = "myUrl";
    Task task = new Task("task");
    Resource res = new Resource("res");
    Mapping<Task, Resource> mapping = PsMappingLocalRes.createLocResMapping(task, res, url);
    VertxProvider vMock = mock(VertxProvider.class);
    FunctionFactoryLocalResources funcFacLocal =
        new FunctionFactoryLocalResources(new HashSet<>(), vMock);
    FunctionFactoryServerless funcServerless =
        new FunctionFactoryServerless(new HashSet<>(), vMock);
    assertTrue(funcFacLocal.isApplicable(new FactoryInputUser(task, mapping)));
    assertFalse(funcServerless.isApplicable(new FactoryInputUser(task, mapping)));
  }
}
