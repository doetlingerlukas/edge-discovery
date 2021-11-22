package edge.discovery.graph;

import at.uibk.dps.ee.core.ModelModificationListener;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentSpecification;
import at.uibk.dps.ee.model.graph.MappingsConcurrent;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import at.uibk.dps.ee.model.properties.*;
import at.uibk.dps.ee.model.properties.PropertyServiceFunction.UsageType;
import com.google.inject.Inject;
import edge.discovery.device.Device;
import edge.discovery.device.DeviceManager;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;
import net.sf.opendse.model.properties.TaskPropertyService;

import java.util.Set;

/**
 * The {@link SpecificationUpdate} updates the specification to account for the
 * discovered and/or configured local resources.
 * 
 * @author Fedor Smirnov
 *
 */
public class SpecificationUpdate {

  protected final EnactmentSpecification spec;
  protected final MappingsConcurrent mappings;
  protected final Set<ModelModificationListener> listeners;

  @Inject
  public SpecificationUpdate(SpecificationProvider specProv,
      Set<ModelModificationListener> listeners) {
    this.spec = specProv.getSpecification();
    this.mappings = spec.getMappings();
    this.listeners = listeners;
  }

  public void addLocalResourceToModel(Device device) {
    // add the resource to the resource graph (one resource node per function
    // deployed on the device)
    String resId = getLocalResourceId(device);
    Resource localResource = PropertyServiceResource.createResource(resId);
    spec.getResourceGraph().addVertex(localResource);

    // add the connection between host and device
    var hostResource = spec.getResourceGraph().getVertex(ConstantsEEModel.idLocalResource);
    PropertyServiceLink.connectResources(spec.getResourceGraph(), hostResource, localResource);

    // add the mappings of application tasks to the device nodes
    spec.getEnactmentGraph().getVertices().stream()
        .filter(node -> TaskPropertyService.isProcess(node))
        .filter(task -> PropertyServiceFunction.getUsageType(task).equals(UsageType.User))
        .forEach(userFunc -> {
          final String url = getTriggerUrl(userFunc, device); 
          final Mapping<Task, Resource> locResMapping = PsMappingLocalRes.createLocResMapping(userFunc, localResource, url);
          mappings.addMapping(locResMapping);
        });

    // trigger the GUI update
    listeners.forEach(ModelModificationListener::reactToModelModification);
  }

  /**
   * Returns the Url used to trigger the execution of the function modeled by the
   * given task on the given device.
   * 
   * @param function the function node
   * @param device the local resource device
   * @return the Url used to trigger the execution of the function modeled by the
   *         given task on the given device
   */
  protected String getTriggerUrl(Task function, Device device) {
    return DeviceManager.getDeviceFunctionUrl(device) + function.toString();
  }

  /**
   * Returns the resource ID generated based on the discovered device
   * 
   * @param device the discovered device
   * @return the id for the resource node created to represent the device
   */
  protected String getLocalResourceId(Device device) {
    return device.getUniqueName();
  }
}
