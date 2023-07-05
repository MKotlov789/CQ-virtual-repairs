package virtualrepair;

import de.tu_dresden.inf.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunction;
import org.semanticweb.owlapi.model.OWLOntology;

import de.tu_dresden.inf.lat.abox_repairs.generator.RepairGenerator;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class VirtualRepairManager{
	private DFunction dFunction;
	private CFunction cFunction;
	public VirtualRepairManager(OWLOntology ont,
								 SeedFunction s,
								 ReasonerFacade reasonerWithoutTBox) throws OWLOntologyCreationException {
		DFunctionHandler dHandler = new DFunctionHandler(ont, reasonerWithoutTBox, s);
		dFunction  = dHandler.computeDFunction();


		CFunctionHandler cHandler = new CFunctionHandler(ont, reasonerWithoutTBox, s, dFunction);
		cFunction = cHandler.computeCFunction();
	}
	public VirtualRepairManager(OWLOntology ont,
								SeedFunction s) throws OWLOntologyCreationException {
		DFunctionHandler dHandler = new DFunctionHandler(ont, s);
		dFunction  = dHandler.computeDFunction();


		CFunctionHandler cHandler = new CFunctionHandler(ont, s, dFunction);
		cFunction = cHandler.computeCFunction();
	}

	public CFunction getcFunction() {
		return cFunction;
	}

	public DFunction getdFunction() {
		return dFunction;
	}
}
