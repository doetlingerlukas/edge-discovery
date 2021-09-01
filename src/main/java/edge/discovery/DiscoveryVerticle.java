package edge.discovery;

import edge.discovery.routes.ReqHandlerDeploy;
import edge.discovery.routes.ReqHandlerRegister;
import edge.discovery.routes.ReqHandlerSearch;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscoveryVerticle extends AbstractVerticle {

  protected final Logger logger = LoggerFactory.getLogger(DiscoveryVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) {
    var router = Router.router(vertx);
    configureRoutes(router);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(Constants.serverPort, http -> {
        if (http.succeeded()) {
          startPromise.complete();
          logger.info("HTTP server started on port " + Constants.serverPort);
        } else {
          startPromise.fail(http.cause());
        }
      });
  }

  protected void configureRoutes(Router router) {
    router.route(Constants.routePathRegistration)
      .method(HttpMethod.POST)
      .handler(BodyHandler.create())
      .blockingHandler(new ReqHandlerRegister());

    router.route(Constants.routePathDeploy)
      .method(HttpMethod.POST)
      .handler(BodyHandler.create())
      .blockingHandler(new ReqHandlerDeploy());

    router.route(Constants.routePathSearch)
      .method(HttpMethod.GET)
      .blockingHandler(new ReqHandlerSearch());
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new DiscoveryVerticle());
  }
}
