/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.jena.model;

/**
 * A technical interface to provide a possibility to assign {@link OntRealProperty data or object} property
 * into {@link OntClass.RestrictionCE restriction class expression}.
 * <p>
 * Created by @ssz on 09.05.2019.
 *
 * @param <P> {@link OntRealProperty data or object} property expression
 * @param <R> - return type, a subtype of {@link OntClass.RestrictionCE}
 * @see HasProperty
 */
interface SetProperty<P extends OntRealProperty, R extends OntClass.RestrictionCE<?>> {

    /**
     * Sets the given property into this Restriction
     * (as an object with predicate {@link com.github.owlcs.ontapi.jena.vocabulary.OWL#onProperty owl:onProperty}
     * if it is Unary Restriction).
     *
     * @param property {@link P}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     */
    R setProperty(P property);
}
