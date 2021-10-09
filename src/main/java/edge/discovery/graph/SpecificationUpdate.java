package edge.discovery.graph;

import at.uibk.dps.ee.core.ModelModificationListener;
import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.graph.EnactmentSpecification;
import at.uibk.dps.ee.model.graph.MappingsConcurrent;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import at.uibk.dps.ee.model.properties.*;
import com.google.inject.Inject;
import edge.discovery.device.Device;
import net.sf.opendse.model.Mapping;
import net.sf.opendse.model.Resource;
import net.sf.opendse.model.Task;

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
  public SpecificationUpdate(SpecificationProvider specProv, Set<ModelModificationListener> listeners) {
    this.spec = specProv.getSpecification();
    this.mappings = spec.getMappings();
    this.listeners = listeners;
  }

  public void addLocalResourceToModel(Device device) {
    // add the resource to the resource graph (one resource node per function deployed on the device)
    var newResource = PropertyServiceResourceServerless.createServerlessResource(device.getName(), device.getAddress().toString());
    spec.getResourceGraph().addVertex(newResource);

    // add the connection between host and device
    var localResource = spec.getResourceGraph().getVertex(ConstantsEEModel.idLocalResource);
    PropertyServiceLink.connectResources(spec.getResourceGraph(), localResource, newResource);
    
    // add the mappings of application tasks to the device nodes
    spec.getEnactmentGraph().getVertices().forEach(t -> {
      mappings.addMapping(PropertyServiceMapping.createMapping(t, newResource, PropertyServiceMapping.EnactmentMode.Serverless,
        t.toString() + device.getName()));
    });

    // trigger the GUI update
    listeners.forEach(ModelModificationListener::reactToModelModification);
  }
}
