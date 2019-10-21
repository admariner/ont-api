/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal.axioms;

import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.HasDomain;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import com.github.owlcs.ontapi.internal.AxiomTranslator;
import com.github.owlcs.ontapi.internal.InternalConfig;
import com.github.owlcs.ontapi.internal.WriteHelper;
import com.github.owlcs.ontapi.jena.model.OntGraphModel;
import com.github.owlcs.ontapi.jena.model.OntPE;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.OntModels;

/**
 * The base class for {@link ObjectPropertyDomainTranslator} and {@link DataPropertyDomainTranslator} and {@link AnnotationPropertyDomainTranslator}.
 * The for mapping statement with {@code rdfs:domain} predicate.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public abstract class AbstractPropertyDomainTranslator<Axiom extends OWLAxiom & HasDomain & HasProperty,
        P extends OntPE> extends AxiomTranslator<Axiom> {
    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        WriteHelper.writeTriple(model, axiom.getProperty(), RDFS.domain, axiom.getDomain(), axiom.annotationsAsList());
    }

    abstract Class<P> getView();

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        return OntModels.listLocalStatements(model, null, RDFS.domain, null).filterKeep(s -> filter(s, config));
    }

    protected boolean filter(OntStatement statement, InternalConfig config) {
        return statement.getSubject().canAs(getView());
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        return RDFS.domain.equals(statement.getPredicate()) && filter(statement, config);
    }
}