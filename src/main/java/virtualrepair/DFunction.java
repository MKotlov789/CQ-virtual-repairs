package virtualrepair;

import java.util.HashMap;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import de.tu_dresden.inf.lat.abox_repairs.repair_type.RepairType;
import virtualrepair.tools.OWLIndClExpressionKey;

public class DFunction extends HashMap<OWLIndClExpressionKey, Set<OWLClassExpression>> {

	public DFunction() {
		super();
	}

}
