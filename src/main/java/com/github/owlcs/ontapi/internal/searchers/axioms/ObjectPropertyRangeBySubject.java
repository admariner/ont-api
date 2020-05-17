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

package com.github.owlcs.ontapi.internal.searchers.axioms;

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.OWLTopObjectType;
import com.github.owlcs.ontapi.internal.axioms.ObjectPropertyRangeTranslator;
import com.github.owlcs.ontapi.jena.model.OntModel;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;

/**
 * Created by @ssz on 17.05.2020.
 */
public class ObjectPropertyRangeBySubject extends BaseByObject<OWLObjectPropertyRangeAxiom, OWLObjectPropertyExpression> {
    private static final ObjectPropertyRangeTranslator TRANSLATOR = getTranslator(OWLTopObjectType.OBJECT_PROPERTY_RANGE);

    @Override
    public ExtendedIterator<ONTObject<OWLObjectPropertyRangeAxiom>> listONTAxioms(OWLObjectPropertyExpression subject,
                                                                                  OntModel model,
                                                                                  ONTObjectFactory factory,
                                                                                  AxiomsSettings config) {
        return translate(TRANSLATOR, listBySubjectAndPredicate(model, asResource(subject), RDFS.range)
                .filterKeep(s -> TRANSLATOR.filter(s, config)), factory, config);
    }
}
