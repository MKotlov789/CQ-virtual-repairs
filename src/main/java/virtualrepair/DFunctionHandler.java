package virtualrepair;

import java.util.Set;
import java.util.stream.Stream;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import com.github.andrewoma.dexx.collection.HashSet;

import de.tu_dresden.inf.lat.abox_repairs.reasoning.ReasonerFacade;
import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunction;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import virtualrepair.tools.OWLIndClExpressionKey;

public class DFunctionHandler {
	private OWLOntology ontology;
	private SeedFunction seedFunction;
	private ReasonerFacade reasonerWithoutTBox;
	private OWLReasonerFactory rf;
	OWLDataFactory df;
	private boolean applicable;
	private OWLReasoner reasoner;
	private OWLDataFactory factory;
	
	public DFunctionHandler(OWLOntology ontology, ReasonerFacade reasonerWithoutTBox, SeedFunction seedFunction) {
		this.ontology = ontology;
		this.seedFunction = seedFunction;
		this.reasonerWithoutTBox = reasonerWithoutTBox;
		this.rf = new ReasonerFactory();
		this.reasoner = rf.createReasoner(ontology);
		this.factory = this.ontology.getOWLOntologyManager().getOWLDataFactory();
	}
	public DFunctionHandler(OWLOntology ontology, SeedFunction seedFunction) {
		this.ontology = ontology;
		this.seedFunction = seedFunction;

		this.reasonerWithoutTBox = null;
		try {
			reasonerWithoutTBox = ReasonerFacade.newReasonerFacadeWithoutTBox(this.ontology,this.seedFunction.getNestedClassExpression());
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.rf = new ReasonerFactory();
		this.df = OWLManager.getOWLDataFactory();
		this.reasoner = rf.createReasoner(ontology);
		this.factory = this.ontology.getOWLOntologyManager().getOWLDataFactory();
	}


	public DFunction computeDFunction() {
		DFunction dFunction = new DFunction();
		this.applicable = true;
		this.seedFunction.entrySet().stream()
		.forEach(e -> seedFunction.get(e.getKey()).getClassExpressions().stream()
				.filter(expr -> expr.getClassExpressionType() == ClassExpressionType.OBJECT_SOME_VALUES_FROM)
				.forEach(expr -> this.defineDForSucc(e.getKey(), (OWLObjectSomeValuesFrom) expr, (OWLObjectSomeValuesFrom) expr, dFunction)));
		DFunction cash = dFunction;
		
		while (this.applicable) {
			this.applicable = false;
			DFunction previousChanges = cash;
			DFunction currentChanges = new DFunction();
			previousChanges.entrySet().stream()
				.forEach(e -> previousChanges.get(e.getKey()).stream()
						.forEach(ce ->ce.asConjunctSet().stream()
								.filter(expr -> expr.getClassExpressionType() == ClassExpressionType.OBJECT_SOME_VALUES_FROM)
								.forEach(expr -> this.defineDForSucc(e.getKey().individual, (OWLObjectSomeValuesFrom) expr, (OWLObjectSomeValuesFrom) e.getKey().classExpression, currentChanges))));
			
			this.mergeDFunctions(dFunction, currentChanges);
			cash = currentChanges;
		}
		return dFunction;
		
	}
	
	private void defineDForSucc(OWLIndividual individual, OWLObjectSomeValuesFrom existentialRestriction, OWLObjectSomeValuesFrom pointer, DFunction dFunction) {
		System.out.println("defining succ for this object:"+individual);

		Stream.concat(ontology.individualsInSignature(),ontology.referencedAnonymousIndividuals())
			.filter(ind -> ontology.containsAxiom(factory.getOWLObjectPropertyAssertionAxiom(existentialRestriction.getProperty(), individual, ind)))
//			.filter( ind -> reasonerWithoutTBox.instanceOf(ind, existentialRestriction.getFiller()))
			.filter( ind -> reasoner.isEntailed(df.getOWLClassAssertionAxiom( existentialRestriction.getFiller(),ind)))
			.forEach(ind ->{
				this.addValueToD(ind, existentialRestriction, pointer, dFunction);
//				if(!dFunction.containsKey(OWLIndClExpressionKey.getKey(ind, pointer))) {
//					this.addValueToD(ind, existentialRestriction, pointer, dFunction);
//				} else {
//					if ( !dFunction.get(OWLIndClExpressionKey.getKey(ind, pointer)).contains(existentialRestriction.getFiller())) {
//						this.addValueToD(ind, existentialRestriction, pointer, dFunction);
//					}
//				}

			});
	}
	
	
	private void addValueToD(OWLIndividual individual,OWLObjectSomeValuesFrom existentialRestriction, OWLObjectSomeValuesFrom pointer,DFunction dFunction) {
		Set<OWLClassExpression> toAdd = new java.util.HashSet<>();
		toAdd.add(existentialRestriction.getFiller());
		dFunction.merge(OWLIndClExpressionKey.getKey(individual, pointer), toAdd,(o,n) ->{o.addAll(n);return o;});
		this.alertChanges();	
	}
	
	private void alertChanges() {
		
		this.applicable = true;
	}
	
	private void mergeDFunctions(DFunction dFunction, DFunction toAdd) {
		toAdd.entrySet().stream().forEach(e -> dFunction.merge(e.getKey(), e.getValue(), (o,n) ->{o.addAll(n);return o;}));
	}
}
