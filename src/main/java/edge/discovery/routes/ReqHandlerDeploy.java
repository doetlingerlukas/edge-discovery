package edge.discovery.routes;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used internally by Apollo to deploy a function to an edge-device.
 *
 * @author Lukas Dötlinger
 */
public class ReqHandlerDeploy implements Handler<RoutingContext> {

  protected final Logger logger = LoggerFactory.getLogger(ReqHandlerDeploy.class);

  @Override
  public void handle(RoutingContext event) {
    var res = event.response();
    var req = event.getBodyAsJson();

    HttpClient client = event.vertx().createHttpClient();

    client.request(HttpMethod.POST, "https://" + req.getString("address") + ":8080/system/function")
      .onSuccess(request -> {
        request
          .response(ar -> {
            if (ar.succeeded()) {
              HttpClientResponse response = ar.result();
              logger.info("Function deployment received status " + response.statusCode());
              res.setStatusCode(response.statusCode()).end();
            }
          })
          .putHeader("content-length", "1000")
          .putHeader("content-type", "application/json")
          .end(new JsonObject()
            .put("service", req.getString("functionName"))
            .put("image", req.getString("image"))
            .put("registryAuth", req.getString("key"))
            .toString());
      });
  }
}
