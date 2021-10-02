package edge.discovery.graph;

import java.util.Set;

import at.uibk.dps.ee.model.constants.ConstantsEEModel;
import at.uibk.dps.ee.model.properties.PropertyServiceLink;
import com.google.inject.Inject;
import at.uibk.dps.ee.core.ModelModificationListener;
import at.uibk.dps.ee.model.graph.EnactmentSpecification;
import at.uibk.dps.ee.model.graph.SpecificationProvider;
import at.uibk.dps.ee.model.properties.PropertyServiceResourceServerless;
import edge.discovery.device.Device;
import edu.uci.ics.jung.graph.util.EdgeType;
import net.sf.opendse.model.Link;

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
    var new_vertex = PropertyServiceResourceServerless.createServerlessResource(device.getName(), device.getAddress().toString());
    spec.getResourceGraph().addVertex(new_vertex);

    // add the connection between host and device
    var local_vertex = spec.getResourceGraph().getVertex(ConstantsEEModel.idLocalResource);
    final var link = new Link("test");
    spec.getResourceGraph().addEdge(link, new_vertex, local_vertex, EdgeType.DIRECTED);
    
    // add the mappings of application tasks to the device nodes
    

    // trigger the GUI update
    listeners.forEach(ModelModificationListener::reactToModelModification);
  }
}
