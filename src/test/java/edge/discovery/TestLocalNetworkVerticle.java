package edge.discovery;

import at.uibk.dps.ee.guice.starter.VertxProvider;
import edge.discovery.device.DeviceManager;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class TestLocalNetworkVerticle {

  @BeforeEach
  void deploy_verticle(VertxTestContext testContext) {
    var vProv = new VertxProvider();
    var vertx = vProv.getVertx();
    var deviceManager = new DeviceManager(vProv);
    var verticle = new LocalNetworkVerticle(deviceManager);

    vertx.deployVerticle(verticle, testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  void verticle_deployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
    testContext.completeNow();
  }


  void register_device(Vertx vertx, VertxTestContext testContext) throws Throwable {
    //vertx.eventBus().consumer(Constants.eventBusName, event -> {
    //  assertEquals(event.body(), "localhost");
    //});

    var client = WebClient.create(vertx);

    client
      .post(Constants.serverPort, "localhost", Constants.routePathRegistration)
      .sendBuffer(Buffer.buffer("localhost".getBytes()))
      .onSuccess(res -> {
        assertEquals(res.statusCode(), 200);
      });
  }
}
