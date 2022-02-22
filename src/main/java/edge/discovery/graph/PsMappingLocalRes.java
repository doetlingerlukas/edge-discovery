package edge.discovery.graph;

import at.uibk.dps.ee.model.properties.PropertyServiceMapping;
import at.uibk.dps.ee.model.properties.PropertyServiceMapping.EnactmentMode;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.AbstractPropertyService;


/**
 * Static method container to manage the attributes of mappings pointing to
 * local resources.
 * 
 * @author Fedor Smirnov
 *
 */
public final class PsMappingLocalRes extends AbstractPropertyService {

  private static final String propNameUrl = Property.Url.name();
  private static final String propNameIsLocResMapping = Property.IsLocResMapping.name();

  /**
   * Properties of local resource mappings
   * 
   * @author Fedor Smirnov
   *
   */
  protected enum Property {
    /**
     * The url used to trigger the execution of the corresponding function on the
     * local resource
     */
    Url,
    /**
     * True iff the mapping edge points to a local resource
     */
    IsLocResMapping
  }

  /**
   * No constructor
   */
  private PsMappingLocalRes() {}

  /**
   * Creates a local resource mapping pointing from the given function to the
   * given local resource.
   * 
   * @param func the function node
   * @param res the resource node
   * @param url the url used to trigger the function execution
   * @return a local resource mapping pointing from the given function to the
   *         given local resource
   */
  public static Mapping<Task, Resource> createLocResMapping(Task func, Resource res, String url) {
    final String mappingId = "LocRes-" + func.getId() + "--" + res.getId();
    Mapping<Task, Resource> result = new Mapping<Task, Resource>(mappingId, func, res);
    makeLocResMapping(result);
    setUrl(result, url);
    PropertyServiceMapping.setEnactmentMode(result, EnactmentMode.Other);
    return result;
  }

  /**
   * Sets the url attribute of the given mapping
   * 
   * @param mapping the given mapping
   * @param url the url to set
   */
  static void setUrl(Mapping<Task, Resource> mapping, String url) {
    mapping.setAttribute(propNameUrl, url);
  }

  /**
   * Returns the url of the given mapping
   * 
   * @param mapping the given mapping
   * @return the url annotated at the given mapping
   */
  public static String getUrl(Mapping<Task, Resource> mapping) {
    return (String) getAttribute(mapping, propNameUrl);
  }

  /**
   * Annotates the given mapping as a local resource mapping
   * 
   * @param mapping the mapping to annotate
   */
  static void makeLocResMapping(Mapping<Task, Resource> mapping) {
    mapping.setAttribute(propNameIsLocResMapping, true);
  }

  /**
   * Returns true iff the given mapping is a local resource mapping
   * 
   * @param mapping the given mapping
   * @return true iff the given mapping is a local resource mapping
   */
  public static boolean isLocResMapping(Mapping<Task, Resource> mapping) {
    if (!isAttributeSet(mapping, propNameIsLocResMapping)) {
      return false;
    }
    return (boolean) getAttribute(mapping, propNameIsLocResMapping);
  }
}
