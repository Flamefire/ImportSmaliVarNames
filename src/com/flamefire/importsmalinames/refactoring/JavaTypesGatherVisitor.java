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

package com.flamefire.importsmalinames.refactoring;

import com.flamefire.importsmalinames.types.JavaClass;
import com.flamefire.importsmalinames.types.JavaMethod;
import com.flamefire.importsmalinames.types.JavaVariable;
import com.flamefire.types.CMethod;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class JavaTypesGatherVisitor extends ASTVisitor {
    public final Stack<String> parentStack = new Stack<String>();
    public final Stack<JavaClass> classStack = new Stack<JavaClass>();
    public final Stack<JavaMethod> methodStack = new Stack<JavaMethod>();
    public final Stack<Integer> anonCtStack = new Stack<Integer>();
    private JavaClass curClass = null;
    private JavaMethod curMethod = null;
    private int curAnonCt = 0;
    private String curParent = "";
    public final Map<String, JavaClass> classes = new HashMap<String, JavaClass>();
    private String pck = "";

    @Override
    public boolean visit(PackageDeclaration node) {
        pck = node.getName() + ".";
        return false;
    }

    private void newClass(String name) {
        parentStack.push(curParent);
        classStack.push(curClass);
        if (!curParent.equals(""))
            curParent += ".";
        curParent += name;
        curClass = new JavaClass(pck + curParent);
        classes.put(curClass.name, curClass);
    }

    private void classEnd() {
        curParent = parentStack.pop();
        curClass = classStack.pop();
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        newClass(node.getName().toString());
        return true;
    }

    @Override
    public void endVisit(TypeDeclaration node) {
        classEnd();
    }

    @Override
    public boolean visit(AnonymousClassDeclaration node) {
        curAnonCt++;
        newClass("$" + curAnonCt);
        return true;
    }

    @Override
    public void endVisit(AnonymousClassDeclaration node) {
        classEnd();
    }

    private String getArrayDim(int dim) {
        String res = "";
        for (int i = 0; i < dim; i++) {
            res += "[]";
        }
        return res;
    }

    private String getTypeFromParam(SingleVariableDeclaration v) {
        String type = v.getType().toString() + getArrayDim(v.getExtraDimensions());
        if (v.isVarargs())
            type += "...";
        return type;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        parentStack.push(curParent);
        methodStack.push(curMethod);
        anonCtStack.push(curAnonCt);
        String name = (node.isConstructor()) ? CMethod.CONSTRUCTOR : node.getName().toString();
        if (!curParent.equals(""))
            curParent += "->";
        curParent += name;
        curMethod = new JavaMethod(name);
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> params = node.parameters();
        for (SingleVariableDeclaration p : params) {
            curMethod.parameters.add(new JavaVariable(p.getName().toString(), getTypeFromParam(p)));
        }
        curClass.methods.add(curMethod);
        curAnonCt = 0;
        return true;
    }

    @Override
    public void endVisit(MethodDeclaration node) {
        curParent = parentStack.pop();
        curMethod = methodStack.pop();
        curAnonCt = anonCtStack.pop();
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
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
}
