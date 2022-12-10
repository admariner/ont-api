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

package com.github.owlcs.ontapi.internal.objects;

import com.github.owlcs.ontapi.AsNode;
import com.github.owlcs.ontapi.internal.ONTObject;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.IRI;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * An {@link IRI} implementation that is also {@link ONTObject}.
 * Created by @ssz on 31.08.2019.
 *
 * @since 2.0.0
 */
@SuppressWarnings("WeakerAccess")
public class ONTIRI extends IRI
        implements ONTSimple, AsNode, ONTObject<IRI> {
    private static final long serialVersionUID = -6990484009590466514L;

    protected ONTIRI(String uri) {
        super(uri);
    }

    @Override
    public IRI getOWLObject() {
        return this;
    }

    @Override
    public Stream<Triple> triples() {
        return Stream.empty();
    }

    @Override
    public Node asNode() {
        return NodeFactory.createURI(getIRIString());
    }

    /**
     * Represents the given {@link IRI OWL-API IRI} as {@link ONTIRI ONT-API IRI}.
     *
     * @param iri {@link IRI}, not {@code null}
     * @return {@link ONTIRI}
     */
    public static ONTIRI asONT(IRI iri) {
        if (iri instanceof ONTIRI) return (ONTIRI) iri;
        return create(iri.getIRIString());
    }

    /**
     * Creates an {@link ONTIRI ONT-API IRI} for the given {@code String}.
     *
     * @param uri {@code String}, not {@code null}
     * @return {@link ONTIRI}
     */
    public static ONTIRI create(String uri) {
        return new ONTIRI(Objects.requireNonNull(uri, "Null URI."));
    }
}
