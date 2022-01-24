package edge.discovery.graph;

import at.uibk.dps.ee.enactables.serverless.ServerlessFunction;
import io.vertx.ext.web.client.WebClient;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;

/**
 * The {@link LocalResFunction} models functions which are executed on local
 * resources. The enactment mechanism is the same as for any function based on a
 * REST API. The used URL depends on the local resource used for the processing
 * of the function.
 * 
 * @author Fedor Smirnov
 *
 */
public class LocalResFunction extends ServerlessFunction {

  /**
   * Same as parent constructor
   */
  public LocalResFunction(Task task, Mapping<Task, Resource> serverlessMapping, WebClient client) {
    super(task, serverlessMapping, client);
  }

  @Override
  protected String getFaaSUrl() {
    return PsMappingLocalRes.getUrl(getMappingOptional().get());
  }
}
