package edge.discovery;

import edge.discovery.device.DeviceManager;
import edge.discovery.routes.ReqHandlerDeploy;
import edge.discovery.routes.ReqHandlerRegister;
import edge.discovery.routes.ReqHandlerSearch;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceRegistrationVerticle extends AbstractVerticle {

  private final DeviceManager deviceManager;

  protected final Logger logger = LoggerFactory.getLogger(DeviceRegistrationVerticle.class);

  public DeviceRegistrationVerticle(DeviceManager deviceManager) {
    this.deviceManager = deviceManager;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    var router = Router.router(vertx);
    configureRoutes(router);

    HttpServer server = vertx.createHttpServer()
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
      .blockingHandler(new ReqHandlerRegister(this.deviceManager));

    router.route(Constants.routePathDeploy)
      .method(HttpMethod.POST)
      .handler(BodyHandler.create())
      .blockingHandler(new ReqHandlerDeploy(this.deviceManager));

    router.route(Constants.routePathSearch)
      .method(HttpMethod.GET)
      .blockingHandler(new ReqHandlerSearch());
  }
}
