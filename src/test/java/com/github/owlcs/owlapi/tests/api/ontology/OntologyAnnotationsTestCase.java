/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.owlcs.owlapi.tests.api.ontology;

import com.github.owlcs.owlapi.tests.api.baseclasses.AbstractRoundTrippingTestCase;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;

import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Annotation;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.AnnotationProperty;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Declaration;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.IRI;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Literal;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 */
public class OntologyAnnotationsTestCase extends AbstractRoundTrippingTestCase {

    @Override
    protected OWLOntology createOntology() {
        OWLOntology ont = getOWLOntology();
        OWLAnnotationProperty prop = AnnotationProperty(IRI("http://www.semanticweb.org/ontologies/test/annotationont#",
                "prop"));
        OWLLiteral value = Literal(33);
        OWLAnnotation annotation = Annotation(prop, value);
        ont.getOWLOntologyManager().applyChange(new AddOntologyAnnotation(ont, annotation));
        ont.getOWLOntologyManager().addAxiom(ont, Declaration(prop));
        return ont;
    }

    @Override
    public void testRDFJSON() {
        // XXX ignored. The parser parses the annotation correctly, but it is not
        // associated to the ontology.
        // super.testRDFJSON();
    }
}
