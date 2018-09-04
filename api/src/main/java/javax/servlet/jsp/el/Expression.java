/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.servlet.jsp.el;


/**
 * <p>The abstract class for a prepared expression.</p>
 *
 * <p>An instance of an Expression can be obtained via from an 
 * ExpressionEvaluator instance.</p>
 *
 * <p>An Expression may or not have done a syntactic parse of the expression.
 * A client invoking the evaluate() method should be ready for the case 
 * where ELParseException exceptions are raised. </p>
 *
 * @deprecated As of JSP 2.1, replaced by {@link javax.el.ValueExpression}
 * @since JSP 2.0
 */
@Deprecated
public abstract class Expression {

    /** 
     * Evaluates an expression that was previously prepared.  In some 
     * implementations preparing an expression involves full syntactic 
     * validation, but others may not do so.  Evaluating the expression may 
     * raise an ELParseException as well as other ELExceptions due to 
     * run-time evaluation.
     *
     * @param vResolver A VariableResolver instance that can be used at 
     *   runtime to resolve the name of implicit objects into Objects.
     * @return The result of the expression evaluation.
     *
     * @exception ELException Thrown if the expression evaluation failed.
     */ 
    public abstract Object evaluate( VariableResolver vResolver )
        throws ELException;
}

