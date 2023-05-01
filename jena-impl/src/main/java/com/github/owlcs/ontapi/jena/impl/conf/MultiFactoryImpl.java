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

package com.github.owlcs.ontapi.jena.impl.conf;

import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.utils.Iterators;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link ObjectFactory Ontology Object Factory} implementation to combine several other factories.
 * <p>
 * Created @ssz on 07.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public class MultiFactoryImpl extends BaseFactoryImpl {
    private final List<ObjectFactory> factories;
    private final OntFinder finder;
    private final OntFilter fittingFilter;

    /**
     * Creates a factory instance.
     *
     * @param finder        {@link OntFinder}, optional, if {@code null} then uses only provided sub-factories to search
     * @param fittingFilter {@link OntFilter}, optional, to trim searching
     * @param factories     array of factories to combine, must not be {@code null} or empty
     */
    public MultiFactoryImpl(OntFinder finder, OntFilter fittingFilter, ObjectFactory... factories) {
        this.finder = finder;
        this.fittingFilter = fittingFilter;
        if (factories.length == 0)
            throw new IllegalArgumentException("Empty factory array");
        this.factories = unbend(factories);
    }

    private static List<ObjectFactory> unbend(ObjectFactory... factories) {
        return Arrays.stream(factories)
                .flatMap(f -> f instanceof MultiFactoryImpl ? ((MultiFactoryImpl) f).factories.stream() : Stream.of(f))
                .collect(Collectors.toList());
    }

    @Override
    public EnhNode wrap(Node node, EnhGraph eg) {
        EnhNode res = createInstance(node, eg);
        if (res != null) return res;
        throw new OntJenaException.Conversion("Can't wrap node " + node + ". Use direct factory.");
    }

    @Override
    public boolean canWrap(Node node, EnhGraph eg) {
        return !(fittingFilter != null && !fittingFilter.test(node, eg))
                && Iterators.anyMatch(listFactories(), f -> f.canWrap(node, eg));
    }

    @Override
    public EnhNode createInstance(Node node, EnhGraph eg) {
        if (fittingFilter != null && !fittingFilter.test(node, eg)) return null;
        return Iterators.findFirst(Iterators.filter(listFactories(), f -> f.canWrap(node, eg))
                .mapWith(f -> f.createInstance(node, eg))).orElse(null);
    }

    @Override
    public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
        if (finder != null) {
            return finder.iterator(eg).mapWith(n -> createInstance(n, eg)).filterDrop(Objects::isNull);
        }
        return Iterators.distinct(Iterators.flatMap(listFactories(), f -> f.iterator(eg)));
    }

    public OntFinder getFinder() {
        return finder;
    }

    public OntFilter getFilter() {
        return fittingFilter;
    }

    /**
     * Lists all sub-factories.
     *
     * @return {@link ExtendedIterator} of {@link ObjectFactory}
     */
    public ExtendedIterator<? extends ObjectFactory> listFactories() {
        return Iterators.create(factories);
    }

}
