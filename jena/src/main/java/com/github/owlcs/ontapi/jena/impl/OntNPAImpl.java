/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.jena.impl;

import com.github.owlcs.ontapi.jena.impl.conf.ObjectFactory;
import com.github.owlcs.ontapi.jena.impl.conf.OntFilter;
import com.github.owlcs.ontapi.jena.impl.conf.OntFinder;
import com.github.owlcs.ontapi.jena.model.OntDataProperty;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntNegativeAssertion;
import com.github.owlcs.ontapi.jena.model.OntObjectProperty;
import com.github.owlcs.ontapi.jena.model.OntRealProperty;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iterators;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.Optional;

/**
 * Implementation of the Negative Property Assertion.
 * <p>
 * Created by @szuev on 15.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntNPAImpl<P extends OntRealProperty, T extends RDFNode>
        extends OntObjectImpl implements OntNegativeAssertion<P, T> {

    private static final OntFinder NPA_FINDER = new OntFinder.ByType(OWL.NegativePropertyAssertion);
    private static final OntFilter NPA_FILTER = OntFilter.BLANK
            .and(new OntFilter.HasPredicate(OWL.sourceIndividual))
            .and(new OntFilter.HasPredicate(OWL.assertionProperty));

    public static ObjectFactory objectNPAFactory = Factories.createCommon(ObjectAssertionImpl.class,
            NPA_FINDER, NPA_FILTER, new OntFilter.HasPredicate(OWL.targetIndividual));
    public static ObjectFactory dataNPAFactory = Factories.createCommon(DataAssertionImpl.class,
            NPA_FINDER, NPA_FILTER, new OntFilter.HasPredicate(OWL.targetValue));
    public static ObjectFactory abstractNPAFactory = Factories.createFrom(NPA_FINDER
            , WithObjectProperty.class
            , WithDataProperty.class);

    public OntNPAImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    public static WithDataProperty create(OntGraphModelImpl model,
                                          OntIndividual source,
                                          OntDataProperty property,
                                          Literal target) {
        Resource res = create(model, source).addProperty(OWL.assertionProperty, property)
                .addProperty(OWL.targetValue, target);
        return model.getNodeAs(res.asNode(), WithDataProperty.class);
    }

    public static WithObjectProperty create(OntGraphModelImpl model,
                                            OntIndividual source,
                                            OntObjectProperty property,
                                            OntIndividual target) {
        Resource res = create(model, source)
                .addProperty(OWL.assertionProperty, property)
                .addProperty(OWL.targetIndividual, target);
        return model.getNodeAs(res.asNode(), WithObjectProperty.class);
    }

    @Override
    public Optional<OntStatement> findRootStatement() {
        return getRequiredRootStatement(this, OWL.NegativePropertyAssertion);
    }

    @Override
    public ExtendedIterator<OntStatement> listSpec() {
        return Iterators.concat(super.listSpec(), listRequired(OWL.sourceIndividual, OWL.assertionProperty, targetPredicate()));
    }

    abstract Class<P> propertyClass();

    abstract Property targetPredicate();

    @Override
    public OntIndividual getSource() {
        return getRequiredObject(OWL.sourceIndividual, OntIndividual.class);
    }

    @Override
    public P getProperty() {
        return getRequiredObject(OWL.assertionProperty, propertyClass());
    }

    private static Resource create(OntModel model, OntIndividual source) {
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL.NegativePropertyAssertion);
        res.addProperty(OWL.sourceIndividual, source);
        return res;
    }

    public static class ObjectAssertionImpl extends OntNPAImpl<OntObjectProperty, OntIndividual> implements WithObjectProperty {
        public ObjectAssertionImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        Class<OntObjectProperty> propertyClass() {
            return OntObjectProperty.class;
        }

        @Override
        Property targetPredicate() {
            return OWL.targetIndividual;
        }

        @Override
        public Class<WithObjectProperty> getActualClass() {
            return WithObjectProperty.class;
        }


        @Override
        public OntIndividual getTarget() {
            return getRequiredObject(targetPredicate(), OntIndividual.class);
        }

    }

    public static class DataAssertionImpl extends OntNPAImpl<OntDataProperty, Literal> implements WithDataProperty {
        public DataAssertionImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        Class<OntDataProperty> propertyClass() {
            return OntDataProperty.class;
        }

        @Override
        Property targetPredicate() {
            return OWL.targetValue;
        }

        @Override
        public Class<WithDataProperty> getActualClass() {
            return WithDataProperty.class;
        }


        @Override
        public Literal getTarget() {
            return getRequiredObject(targetPredicate(), Literal.class);
        }
    }
}
