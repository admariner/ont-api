package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * object property with owl:AsymmetricProperty type
 *
 * Created by @szuev on 18.10.2016.
 */
class AsymmetricObjectPropertyTranslator extends AbstractPropertyTypeTranslator<OWLAsymmetricObjectPropertyAxiom, OntOPE> {

    @Override
    Resource getType() {
        return OWL.AsymmetricProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    Wrap<OWLAsymmetricObjectPropertyAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory(statement.getModel());
        Wrap<OWLObjectPropertyExpression> p = ReadHelper.getObjectProperty(getSubject(statement), df);
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLAsymmetricObjectPropertyAxiom res = df.getOWLAsymmetricObjectPropertyAxiom(p.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p);
    }
}
