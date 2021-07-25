package edge.discovery;

import edge.discovery.routes.ReqHandlerRegister;
import edge.discovery.routes.ReqHandlerSearch;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    var router = Router.router(vertx);
    configureRoutes(router);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(Constants.serverPort, http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port " + Constants.serverPort);
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

    router.route(Constants.routePathSearch)
      .method(HttpMethod.GET)
      .blockingHandler(new ReqHandlerSearch());
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
}
