package edge.discovery.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import at.uibk.dps.ee.enactables.FunctionAbstract;
import at.uibk.dps.ee.enactables.serverless.ServerlessFunction;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;

/**
 * The {@link LocalNetworkFunction} models functions which are executed on
 * resources in the local network. The enactment mechanism is based on a REST
 * API. The used URL depends on the local resource used for the processing of
 * the function.
 * 
 * @author Fedor Smirnov
 *
 */
public class LocalNetworkFunction extends FunctionAbstract {

  protected final WebClient client;

  protected final Logger logger = LoggerFactory.getLogger(ServerlessFunction.class);

  /**
   * The constructor used by the factory
   * 
   * @param task the task that is associated with the function
   * @param localMapping the mapping of the task
   * @param client the web client used for the requests
   */
  public LocalNetworkFunction(Task task, Mapping<Task, Resource> localMapping, WebClient client) {
    super(task, localMapping);
    this.client = client;
  }

  /**
   * Returns the URL used to trigger the function execution on a local resource.
   * 
   * @return the URL used to trigger the function execution on a local resource
   */
  protected String getFunctionUrl() {
    return PsMappingLocalRes.getUrl(getMappingOptional().get());
  }

  @Override
  public Future<JsonObject> processVerifiedInput(final JsonObject input) {
    final String url = getFunctionUrl();
    final Promise<JsonObject> resultPromise = Promise.promise();
    final Future<HttpResponse<Buffer>> futureResponse =
        client.postAbs(url).sendJson(new io.vertx.core.json.JsonObject(input.toString()));
    logger.info("Serverless function {} triggerred.", url);
    futureResponse.onSuccess(asyncRes -> {
      logger.info("Serverless function {} finished", url);
      final JsonObject resultJson =
          JsonParser.parseString(asyncRes.body().toString()).getAsJsonObject();
      resultPromise.complete(resultJson);
    }).onFailure(failureThrowable -> resultPromise.fail(failureThrowable));
    return resultPromise.future();
  }
}
