package virtualrepair.tools;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

public class OWLIndClExpressionKey {
	
	public OWLIndividual individual;
	public OWLClassExpression classExpression;
	
	public OWLIndClExpressionKey(OWLIndividual individual, OWLClassExpression classExpression ) {
		
		this.individual = individual;
		this.classExpression = classExpression;
	}
	
	public static OWLIndClExpressionKey getKey(OWLIndividual individual, OWLClassExpression classExpression ) {
		return new OWLIndClExpressionKey(individual,classExpression);
	}
	
	public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
 
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
 
        OWLIndClExpressionKey key = (OWLIndClExpressionKey) o;
        if (individual != null ? !individual.equals(key.individual) : key.individual != null) {
            return false;
        }
 
        if (classExpression != null ? !classExpression.equals(key.classExpression) : key.classExpression != null) {
            return false;
        }
 
        return true;
    }
 
    @Override
    public int hashCode()
    {
        int result = individual != null ? individual.hashCode() : 0;
        result = 31 * result + (classExpression != null ? classExpression.hashCode() : 0);
        return result;
    }
 
    @Override
    public String toString() {
        return individual + "_" + classExpression;
    }

}
