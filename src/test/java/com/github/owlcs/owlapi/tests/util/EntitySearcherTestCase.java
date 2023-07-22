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

package com.github.owlcs.owlapi.tests.util;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntitySearcherTestCase extends TestBase {

    private OWLObjectProperty superProperty;
    private OWLObjectProperty subProperty;
    private Set<OWLOntology> ontologies;

    @BeforeEach
    public void setUp() {
        PrefixManager pm = new DefaultPrefixManager();
        pm.setDefaultPrefix("http://www.ontologies.com/ontology");
        subProperty = OWLFunctionalSyntaxFactory.ObjectProperty("subProperty", pm);
        superProperty = OWLFunctionalSyntaxFactory.ObjectProperty("superProperty", pm);
        OWLOntology ontology = OWLFunctionalSyntaxFactory.Ontology(m, OWLFunctionalSyntaxFactory.SubObjectPropertyOf(subProperty, superProperty));
        ontologies = Collections.singleton(ontology);
    }

    @Test
    public void testShouldReturnSuperProperty() {
        List<OWLObjectPropertyExpression> supers = EntitySearcher.getSuperProperties(subProperty, ontologies.stream())
                .collect(Collectors.toList());
        Assertions.assertTrue(supers.contains(superProperty));
    }

    @Test
    public void testShouldReturnSubProperty() {
        Stream<OWLObjectPropertyExpression> subs = EntitySearcher.getSubProperties(superProperty, ontologies.stream());
        Assertions.assertTrue(subs.anyMatch(x -> x.equals(subProperty)));
    }
}
