/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.owlcs.owlapi.tests.api.annotations;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
public class AnnotationShortFormProviderTestCase extends TestBase {

    private final PrefixManager pm = new DefaultPrefixManager(null, null, "http://org.semanticweb.owlapi/ont#");
    private final OWLAnnotationProperty prop = OWLFunctionalSyntaxFactory.AnnotationProperty("prop", pm);
    private final List<OWLAnnotationProperty> props = CollectionFactory.list(prop);
    private final Map<OWLAnnotationProperty, List<String>> langMap = new HashMap<>();

    @Test
    public void testLiteralWithoutLanguageValue() {
        OWLNamedIndividual root = OWLFunctionalSyntaxFactory.NamedIndividual("ind", pm);
        String shortForm = "MyLabel";
        OWLFunctionalSyntaxFactory.Ontology(m, OWLFunctionalSyntaxFactory
                .AnnotationAssertion(prop, root.getIRI(),
                        OWLFunctionalSyntaxFactory.Literal(shortForm)));
        AnnotationValueShortFormProvider sfp = new AnnotationValueShortFormProvider(props, langMap, m);
        Assert.assertEquals(sfp.getShortForm(root), shortForm);
    }

    @Test
    public void testLiteralWithLanguageValue() {
        OWLNamedIndividual root = OWLFunctionalSyntaxFactory.NamedIndividual("ind", pm);
        String label1 = "MyLabel";
        String label2 = "OtherLabel";
        OWLFunctionalSyntaxFactory.Ontology(m, OWLFunctionalSyntaxFactory.AnnotationAssertion(prop, root.getIRI(),
                OWLFunctionalSyntaxFactory.Literal(label1, "ab")),
                OWLFunctionalSyntaxFactory.AnnotationAssertion(prop, root.getIRI(),
                        OWLFunctionalSyntaxFactory.Literal(label2, "xy")));
        langMap.put(prop, Arrays.asList("ab", "xy"));
        AnnotationValueShortFormProvider sfp = new AnnotationValueShortFormProvider(props, langMap, m);
        Assert.assertEquals(sfp.getShortForm(root), label1);
        Map<OWLAnnotationProperty, List<String>> langMap2 = new HashMap<>();
        langMap2.put(prop, Arrays.asList("xy", "ab"));
        AnnotationValueShortFormProvider sfp2 = new AnnotationValueShortFormProvider(props, langMap2, m);
        Assert.assertEquals(sfp2.getShortForm(root), label2);
    }

    @Test
    public void testIRIValue() {
        OWLNamedIndividual root = OWLFunctionalSyntaxFactory.NamedIndividual("ind", pm);
        OWLFunctionalSyntaxFactory.Ontology(m, OWLFunctionalSyntaxFactory.AnnotationAssertion(prop, root.getIRI(),
                IRI.create("http://org.semanticweb.owlapi/ont#", "myIRI")));
        AnnotationValueShortFormProvider sfp = new AnnotationValueShortFormProvider(props, langMap, m);
        Assert.assertEquals("myIRI", sfp.getShortForm(root));
    }

    @Test
    public void testShouldWrapWithDoubleQuotes() {
        OWLNamedIndividual root = OWLFunctionalSyntaxFactory.NamedIndividual("ind", pm);
        String shortForm = "MyLabel";
        OWLFunctionalSyntaxFactory.Ontology(m, OWLFunctionalSyntaxFactory.AnnotationAssertion(prop,
                root.getIRI(), OWLFunctionalSyntaxFactory.Literal(shortForm)));
        AnnotationValueShortFormProvider sfp = new AnnotationValueShortFormProvider(m,
                new SimpleShortFormProvider(), new SimpleIRIShortFormProvider(), props, langMap);
        sfp.setLiteralRenderer(new StringAnnotationVisitor() {

            @SuppressWarnings("NullableProblems")
            @Override
            public String visit(OWLLiteral literal) {
                return '"' + literal.getLiteral() + '"';
            }
        });
        String shortForm2 = sfp.getShortForm(root);
        Assert.assertEquals(shortForm2, '"' + shortForm + '"');
    }
}
