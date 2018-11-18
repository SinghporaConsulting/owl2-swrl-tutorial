package cz.makub;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.search.EntitySearcher;

import com.google.common.collect.Multimap;

import openllet.aterm.ATermAppl;
import openllet.core.boxes.abox.ABox;
import openllet.core.boxes.abox.Literal;
import openllet.core.rules.builtins.BuiltInRegistry;
import openllet.core.rules.builtins.GeneralFunction;
import openllet.core.rules.builtins.GeneralFunctionBuiltIn;
import openllet.core.utils.TermFactory;
import openllet.owlapi.OpenlletReasonerFactory;

/**
 * Example of Pellet custom SWRL built-in.
 * <p>
 * Run in Maven with <code>mvn exec:java -Dexec.mainClass=cz.makub.SWRLBuiltInsTutorial</code>
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class SWRLBuiltInsTutorial {
	openllet.core.rules.builtins.NumericOperators x;
	openllet.core.rules.builtins.NumericAdapter y;
    /**
     * Implementation of ThisYear SWRL custom built-in.
     * (Incorporated from: https://github.com/martin-kuba/owl2-swrl-tutorial
     *   under licence as of 26-10-2018) 
     */
    private static class ThisYear implements GeneralFunction {

        public boolean apply(ABox abox, Literal[] args) {
            Calendar calendar = Calendar.getInstance();
            String year = new SimpleDateFormat("yyyy").format(calendar.getTime());
            if (args[0] == null) {
                //variable not bound, fill it with the current year
        		ATermAppl term = TermFactory.literal(Integer.parseInt(year));
            	args[0] = abox.addLiteral(term);
            	return args[0] != null;
            } else {
                //variable is bound, compare its value with the current year
                return year.equals(args[0].getLexicalValue());
            }
        }

        public boolean isApplicable(boolean[] boundPositions) {
            //the built-in is applicable for one argument only
            return boundPositions.length == 1;
        }

    }

    private static final String DOC_URL = "http://acrab.ics.muni.cz/ontologies/swrl_tutorial.owl";
 
    public static void main(String[] args) throws OWLOntologyCreationException {
        //register my built-in
        BuiltInRegistry.instance.registerBuiltIn("urn:makub:builtIn#thisYear", new GeneralFunctionBuiltIn(new ThisYear()));
        //initialize ontology and reasoner
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(IRI.create(DOC_URL));
        OWLReasonerFactory reasonerFactory = OpenlletReasonerFactory.getInstance();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
        OWLDataFactory factory = manager.getOWLDataFactory();
        PrefixDocumentFormat pm = manager.getOntologyFormat(ontology).asPrefixOWLDocumentFormat();
        //use the rule with the built-in to infer data property values
        OWLNamedIndividual martin = factory.getOWLNamedIndividual(":Martin", pm);
        listAllDataPropertyValues(martin, ontology, reasoner);

        OWLNamedIndividual ivan = factory.getOWLNamedIndividual(":Ivan", pm);
        listAllDataPropertyValues(ivan, ontology, reasoner);
    }

    static void listAllDataPropertyValues(OWLNamedIndividual individual, OWLOntology ontology, OWLReasoner reasoner) {
        OWLObjectRenderer renderer = new DLSyntaxObjectRenderer();
        Multimap<OWLDataPropertyExpression, OWLLiteral> assertedValues = EntitySearcher.getDataPropertyValues(individual, ontology);
        for (OWLDataProperty dataProp : ontology.getDataPropertiesInSignature(Imports.INCLUDED)) {
            for (OWLLiteral literal : reasoner.getDataPropertyValues(individual, dataProp)) {
                Collection<OWLLiteral> literalSet = assertedValues.get(dataProp);
                boolean asserted = (literalSet != null && literalSet.contains(literal));
                System.out.println((asserted ? "asserted" : "inferred") + " data property for " + renderer.render(individual) + " : "
                        + renderer.render(dataProp) + " -> " + renderer.render(literal));
            }
        }
    }
}