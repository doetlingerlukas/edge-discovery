package edge.discovery.graph;

import java.util.Set;
import com.google.inject.Inject;
import at.uibk.dps.ee.core.ModelModificationListener;
import at.uibk.dps.ee.model.graph.EnactmentSpecification;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceResourceServerless;
import edge.discovery.device.Device;

/**
 * The {@link SpecificationUpdate} updates the specification to account for the
 * discovered and/or configured local resources.
 * 
 * @author Fedor Smirnov
 *
 */
public class SpecificationUpdate {

  protected final EnactmentSpecification spec;
  protected final Set<ModelModificationListener> listeners;
  
  @Inject
  public SpecificationUpdate(SpecificationProvider specProv, Set<ModelModificationListener> listeners) {
    this.spec = specProv.getSpecification();
    this.listeners = listeners;
  }
  
  
  public void addLocalResourceToModel(Device device) {
    // add the resource to the resource graph (one resource node per function deployed on the device)
    spec.getResourceGraph().addVertex(PropertyServiceResourceServerless.createServerlessResource(device.getName(), device.getAddress().toString()));
    
    
    // add the connection between host and device
    
    
    // add the mappings of application tasks to the device nodes
    
    
    
    // trigger the GUI update
    listeners.forEach(listener -> listener.reactToModelModification());
  }
  
}
