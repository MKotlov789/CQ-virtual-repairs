package virtualrepair;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.sparql.core.Var;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;

import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunction;
import virtualrepair.tools.OWLIndClExpressionKey;

public class VirtualRepairAnswerFilter {
	public QuerySolution querySolution;
	private Set<Var> answerVariables;
	private Set<Triple> triplePatterns;
	private SeedFunction seedFunction;
	private DFunction dFunction;
	private CFunction cFunction;
	private OWLDataFactory factory;

	public VirtualRepairAnswerFilter(Set<Var> answerVariables, Set<Triple> triplePatterns,
                                     SeedFunction seedFunction, DFunction dFunction, CFunction cFunction) {
		this.answerVariables = answerVariables;
		this.cFunction = cFunction;
		this.dFunction = dFunction;
		this.seedFunction = seedFunction;
		this.triplePatterns = triplePatterns;
		this.factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		System.out.println("An answer repairer is generated for a homomorphism, that maps to the answer tuple "+answerVariables);
	}

	public void setQuerySolution(QuerySolution querySolution) {
		this.querySolution = querySolution;
	}
	
	private Set<Var> getSetSucc(OWLObjectSomeValuesFrom exRestriction, Var var, OWLClassExpression pointer) {
		Set<Var> succ = new HashSet<>();
		String property = exRestriction.getProperty().toString();

		for (Triple triple : this.triplePatterns) {
			String prop = "<" + triple.getPredicate().getURI() + ">";
			if (triple.getSubject().equals(var) && prop.equals(property)) {
				OWLNamedIndividual ind = this.factory
						.getOWLNamedIndividual(this.querySolution.get(triple.getObject().getName()).toString());
				if (this.dFunction.containsKey(OWLIndClExpressionKey.getKey(ind, pointer))) {
					if (this.dFunction.get(OWLIndClExpressionKey.getKey(ind, pointer))
							.contains(exRestriction.getFiller())) {
						succ.add((Var) triple.getObject());
					}
				}
			}
		}

//		System.out.println(succ);
		return succ;

	}

	public boolean ifHide() {

		for (Var var : this.answerVariables) {
			OWLNamedIndividual ind = this.factory
					.getOWLNamedIndividual(this.querySolution.get(var.getName()).toString());
			if (this.seedFunction.containsKey(ind)) {
				for (OWLClassExpression cle :this.seedFunction.get(ind).getClassExpressions()) {
					if (cle.isOWLClass()) {
						for (Triple triple : this.triplePatterns) {
//							System.out.println(triple.getPredicate().toString());
							if (triple.getMatchPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
									&& triple.getSubject().getName().equals(var.getName())) {
								OWLClassExpression cl = this.factory.getOWLClass(triple.getObject().getURI());
								if (cl.equals(cle)) {
									return true;
								}
							}
						}

					} else {
						if (this.inst((OWLObjectSomeValuesFrom) cle, var, cle)) {
							System.out.println("INST("+cle+") returned TRUE");
							return true;
						}

					}
				}
			}
		}

		return false;

	}

	public Boolean inst(OWLObjectSomeValuesFrom exRestriction, Var var, OWLClassExpression pointer) {
		Set<Var> succ = this.getSetSucc(exRestriction, var, pointer);
		Set<Var> toRemove = new HashSet<>();
		for (Var v : succ) {
			OWLNamedIndividual ind = this.factory.getOWLNamedIndividual(this.querySolution.get(v.getName()).toString());
//			System.out.println(ind);
			if (this.answerVariables.contains(v)) {
				if (this.cFunction.containsKey(OWLIndClExpressionKey.getKey(ind, exRestriction.getFiller()))) {
					toRemove.add(v);
				} else {
					return true;
				}
			} else {
//				System.out.println(toRemove);
				Set<OWLClassExpression> clExprs = new HashSet<>();
				for (Triple triple : this.triplePatterns) {
//					System.out.println(triple.getPredicate().toString());
					if (triple.getMatchPredicate().toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
							&& triple.getSubject().getName().equals(v.getName())) {
						OWLClassExpression cl = this.factory.getOWLClass(triple.getObject().getURI());
						clExprs.add(cl);
					}
				}
//					System.out.println(toRemove);

				if (!exRestriction.getFiller().asConjunctSet().equals(clExprs)) {
					toRemove.add(v);
				}
				System.out.println("procedure INST, list of ind's excluded from succ"+toRemove);
				if (!toRemove.contains(v)) {
					exRestriction.getFiller().asConjunctSet().stream()
							.filter(ce -> ce.getClassExpressionType() == ClassExpressionType.OBJECT_SOME_VALUES_FROM)
							.filter(ce -> !this.inst((OWLObjectSomeValuesFrom) ce, v, pointer))
							.forEach(ce -> toRemove.add(v));
				}

			}
		}
		System.out.println("procedure INST, list of ind's excluded from succ"+toRemove);
		if (toRemove.equals(succ)) {
			return false;
		} else {
			return true;
		}

	}

}
