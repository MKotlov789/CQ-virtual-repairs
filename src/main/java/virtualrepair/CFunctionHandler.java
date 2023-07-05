package virtualrepair;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;

import de.tu_dresden.inf.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunction;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import virtualrepair.tools.OWLIndClExpressionKey;

public class CFunctionHandler {

	private OWLOntology ontology;
	private SeedFunction seedFunction;
	private ReasonerFacade reasonerWithoutTBox;

	private DFunction dFunction;
	private Boolean applicable;
	private OWLDataFactory factory;
	
	public CFunctionHandler(OWLOntology ontology , ReasonerFacade reasonerWithoutTBox, SeedFunction seedFunction, DFunction dFunction) throws OWLOntologyCreationException {
		this.ontology = ontology;
		this.seedFunction = seedFunction;
		this.reasonerWithoutTBox = reasonerWithoutTBox;

		this.factory = this.ontology.getOWLOntologyManager().getOWLDataFactory();
		this.dFunction = dFunction;
	}
	public CFunctionHandler(OWLOntology ontology, SeedFunction seedFunction, DFunction dFunction) throws OWLOntologyCreationException {
		this.ontology = ontology;
		this.seedFunction = seedFunction;

		try {
			reasonerWithoutTBox = ReasonerFacade.newReasonerFacadeWithoutTBox(this.ontology,this.seedFunction.getNestedClassExpression());
		} catch (
				OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.factory = this.ontology.getOWLOntologyManager().getOWLDataFactory();
		this.dFunction = dFunction;
	}


	public CFunction computeCFunction() {
		
		CFunction cFunction = new CFunction();
		
		dFunction.entrySet().stream().filter(e -> seedFunction.containsKey(e.getKey().individual))
			.forEach(e -> e.getValue().stream()
					.forEach(ce ->  {
//						cFunction.put(new OWLIndClExpressionKey(e.getKey().individual, ce), true);
						seedFunction.get(e.getKey().individual).getClassExpressions().stream()
									.forEach(с ->{
										if( this.reasonerWithoutTBox.subsumedBy(ce, с)) {
											cFunction.put(new OWLIndClExpressionKey(e.getKey().individual, ce), false);
										}
									}
											);
									}
							)
					);
		return cFunction;
		
	}

	
}
