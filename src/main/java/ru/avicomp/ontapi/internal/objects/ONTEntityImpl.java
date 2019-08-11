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

package ru.avicomp.ontapi.internal.objects;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.model.OntGraphModel;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A base {@link OWLEntity} implementation which has a reference to a model.
 * Created by @ssz on 07.08.2019.
 *
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTEntityImpl extends ONTResourceImpl implements OWLEntity {
    private static final long serialVersionUID = -4341538961681314285L;

    protected ONTEntityImpl(String uri, Supplier<OntGraphModel> m) {
        super(uri, m);
    }

    @Override
    public String getURI() {
        return (String) node;
    }

    @Override
    public Node asNode() {
        return NodeFactory.createURI(getURI());
    }

    @Override
    public abstract OntEntity asResource();

    @Override
    public IRI getIRI() {
        return getObjectFactory().toIRI(getURI());
    }

    @Override
    public String toStringID() {
        return getURI();
    }

    @Override
    public boolean isBuiltIn() {
        return asResource().isBuiltIn();
    }

    @Override
    public Set<OWLEntity> getSignatureSet() {
        return createSet(this);
    }

    @Override
    public boolean containsEntityInSignature(OWLEntity entity) {
        return equals(entity);
    }

    @Override
    protected Set<OWLClass> getNamedClassSet() {
        return createSet();
    }

    @Override
    protected Set<OWLDatatype> getDatatypeSet() {
        return createSet();
    }

    @Override
    protected Set<OWLNamedIndividual> getNamedIndividualSet() {
        return createSet();
    }

    @Override
    protected Set<OWLDataProperty> getDataPropertySet() {
        return createSet();
    }

    @Override
    protected Set<OWLObjectProperty> getObjectPropertySet() {
        return createSet();
    }

    @Override
    protected Set<OWLAnnotationProperty> getAnnotationPropertySet() {
        return createSet();
    }

    @Override
    protected Set<OWLClassExpression> getClassExpressionSet() {
        return createSet();
    }

    @Override
    protected Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
        return createSet();
    }

    protected boolean equals(Resource uri) {
        return uri.getURI().equals(node);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OWLEntity)) {
            return false;
        }
        OWLEntity entity = (OWLEntity) obj;
        if (typeIndex() != entity.typeIndex()) {
            return false;
        }
        if (entity instanceof ONTResourceImpl) {
            return sameAs((ONTResourceImpl) entity);
        }
        if (hashCode != 0 && entity.hashCode() != hashCode) {
            return false;
        }
        return getIRI().equals(entity.getIRI());
    }
}