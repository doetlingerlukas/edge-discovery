package edge.discovery;

/**
 * Class containing constants for the edge discovery module.
 *
 * @author Lukas Dötlinger
 */
public final class Constants {

  public static final int serverPort = 5888;
  public static final String eventBusName = "new.devices";

  // Routes
  public static final String routePathRegistration = "/register/";
  public static final String routePathDeploy = "/deploy/";
  public static final String routePathSearch = "/search/";

  public static final int broadcastPort = 5999;
  public static final String broadcastAvailableMessage = "apollo-available";
  public static final String broadcastReleaseMessage = "apollo-release";

}
