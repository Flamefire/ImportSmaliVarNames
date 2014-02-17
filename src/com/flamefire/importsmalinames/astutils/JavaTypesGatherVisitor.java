/*******************************************************************************
 * Copyright (C) 2014 Flamefire
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.flamefire.importsmalinames.astutils;

import com.flamefire.importsmalinames.types.JavaClass;
import com.flamefire.importsmalinames.types.JavaVariable;

import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.HashMap;
import java.util.Iterator;

public class JavaTypesGatherVisitor extends TypeTraceVisitor {

    public JavaTypesGatherVisitor() {
        super(new HashMap<String, JavaClass>());
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        if (curMethod == null)
            return true;
        String type = node.getType().toString();
        for (@SuppressWarnings("unchecked")
        Iterator<VariableDeclarationFragment> iter = node.fragments().iterator(); iter.hasNext();) {
            VariableDeclarationFragment var = iter.next();
            // VariableDeclarationFragment: is the plain variable declaration
            // part.
            // Example: "int x=0, y=0;" contains two
            // VariableDeclarationFragments, "x=0" and "y=0"
            curMethod.variables.add(new JavaVariable(var.getName().toString(), type
                    + getArrayDim(var.getExtraDimensions())));
        }
        return true;
    }

    @Override
    public boolean visit(Initializer node) {
        return false;
    }

    @Override
    public boolean visit(SingleVariableDeclaration node) {
        // Filter out parameters
        String name = node.getName().toString();
        for (JavaVariable p : curMethod.parameters)
            if (p.name.equals(name))
                return true;
        String type = node.getType().toString() + getArrayDim(node.getExtraDimensions());
        curMethod.variables.add(new JavaVariable(name, type));
        return true;
    }
}
