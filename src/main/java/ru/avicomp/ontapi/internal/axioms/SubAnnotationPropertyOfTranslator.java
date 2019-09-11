/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.internal.axioms;

import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntStatement;

import java.util.Collection;

/**
 * See {@link AbstractSubPropertyTranslator}.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class SubAnnotationPropertyOfTranslator extends AbstractSubPropertyTranslator<OWLSubAnnotationPropertyOfAxiom, OntNAP> {

    @Override
    OWLPropertyExpression getSubProperty(OWLSubAnnotationPropertyOfAxiom axiom) {
        return axiom.getSubProperty();
    }

    @Override
    OWLPropertyExpression getSuperProperty(OWLSubAnnotationPropertyOfAxiom axiom) {
        return axiom.getSuperProperty();
    }

    @Override
    Class<OntNAP> getView() {
        return OntNAP.class;
    }

    /**
     * Returns {@link OntStatement}s defining the {@link OWLSubAnnotationPropertyOfAxiom} axiom.
     *
     * @param model  {@link OntGraphModel}
     * @param config {@link InternalConfig}
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     */
    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        if (!config.isLoadAnnotationAxioms()) return NullIterator.instance();
        return super.listStatements(model, config);
    }

    @Override
    protected boolean filter(OntStatement statement, InternalConfig config) {
        return super.filter(statement, config)
                && ReadHelper.testAnnotationAxiomOverlaps(statement, config,
                AxiomType.SUB_OBJECT_PROPERTY, AxiomType.SUB_DATA_PROPERTY);
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        return config.isLoadAnnotationAxioms() && super.testStatement(statement, config);
    }

    @Override
    public ONTObject<OWLSubAnnotationPropertyOfAxiom> toAxiom(OntStatement statement,
                                                              InternalObjectFactory reader,
                                                              InternalConfig config) {
        ONTObject<OWLAnnotationProperty> sub = reader.getProperty(statement.getSubject(OntNAP.class));
        ONTObject<OWLAnnotationProperty> sup = reader.getProperty(statement.getObject().as(OntNAP.class));
        Collection<ONTObject<OWLAnnotation>> annotations = reader.getAnnotations(statement, config);
        OWLSubAnnotationPropertyOfAxiom res = reader.getOWLDataFactory()
                .getOWLSubAnnotationPropertyOfAxiom(sub.getOWLObject(), sup.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(sub).append(sup);
    }

}
