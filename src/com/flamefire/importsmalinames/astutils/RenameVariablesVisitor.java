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
import com.flamefire.importsmalinames.types.JavaMethod;
import com.flamefire.importsmalinames.types.JavaVariable;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

public class RenameVariablesVisitor extends TypeTraceVisitor {
    private final Map<JavaMethod, Map<String, String>> renamings;
    private final ASTRewrite astRewrite;
    private Map<String, String> renaming = new HashMap<String, String>();
    private final Stack<Map<String, String>> renamingStack = new Stack<Map<String, String>>();

    public RenameVariablesVisitor(Map<String, JavaClass> classes, Map<JavaMethod, Map<String, String>> renamings,
            ASTRewrite astRewrite) {
        super(classes);
        this.renamings = renamings;
        this.astRewrite = astRewrite;
    }

    @Override
    protected void newClassFound(ASTNode node, String name) {
        super.newClassFound(node, name);
        renamingStack.push(renaming);
    }

    @Override
    protected void classEnd() {
        super.classEnd();
        renaming = renamingStack.pop();
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        renamingStack.push(renaming);
        return super.visit(node);
    }

    @Override
    public void endVisit(MethodDeclaration node) {
        super.endVisit(node);
        renaming = renamingStack.pop();
    }

    // Gets a just found class
    // Returns the class that will be new curClass
    @Override
    protected JavaClass handleNewClass(ASTNode node, JavaClass jClass) {
        JavaClass res = classes.get(jClass.name);
        if (res == null) {
            System.err.println("Class " + jClass.name + " not found");
        }
        return res;
    }

    private JavaMethod matchMethod(JavaMethod jMethod) {
        if (curClass == null)
            return null;
        int pCount = jMethod.parameters.size();
        for (JavaMethod m : curClass.methods) {
            if (m.parameters.size() != pCount)
                continue;
            boolean match = true;
            for (int i = 0; i < pCount; i++) {
                JavaVariable p1 = jMethod.parameters.get(i);
                JavaVariable p2 = m.parameters.get(i);
                match = p1.name.equals(p2.name) && p1.getType().equals(p2.getType());
                if (!match)
                    break;
            }
            if (match)
                return m;
        }
        return null;
    }

    // Gets a just found method (With parameters set)
    // Returns the method that will be new curMethod
    @Override
    protected JavaMethod handleNewMethod(MethodDeclaration node, JavaMethod jMethod) {
        JavaMethod res = matchMethod(jMethod);
        if (res == null) {
            System.err.println("Method " + jMethod.name + " not found");
        }
        return res;
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        for (@SuppressWarnings("unchecked")
        Iterator<VariableDeclarationFragment> iter = node.fragments().iterator(); iter.hasNext();) {
            VariableDeclarationFragment var = iter.next();
            // Use empty string to distinguish between fields and variables
            renaming.put(var.getName().toString(), "");
        }
        return true;
    }

    private String getNewNameForDecl(String oldName) {
        if (curMethod == null)
            return null;
        Map<String, String> newNames = renamings.get(curMethod);
        if (newNames == null)
            return null;
        String newName = newNames.get(oldName);
        if (newName == null)
            return null;
        int i = 1;
        if (renaming.containsKey(newName) && renaming.get(newName) == null) {
            // Other variable with same name exists
            do {
                i++;
            } while (renaming.containsKey(newName + i) && renaming.get(newName + i) == null);
        }
        if (renaming.containsValue(newName)) {
            // Other variable will be renamed to same name
            do {
                i++;
            } while (renaming.containsValue(newName + i));
        }
        if (i > 1)
            newName += i;
        return newName;
    }

    @Override
    public boolean visit(SingleVariableDeclaration node) {
        String name = node.getName().toString();
        renaming.put(name, getNewNameForDecl(name));
        return true;
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        for (@SuppressWarnings("unchecked")
        Iterator<VariableDeclarationFragment> iter = node.fragments().iterator(); iter.hasNext();) {
            VariableDeclarationFragment var = iter.next();
            String name = var.getName().toString();
            renaming.put(name, getNewNameForDecl(name));
        }
        return true;
    }

    @Override
    public boolean visit(SimpleName node) {
        // We have to be inside a method
        if (curMethod == null)
            return false;
        // Only replace variables
        IBinding binding = node.resolveBinding();
        if (binding.getKind() != IBinding.VARIABLE)
            return false;
        // Check if we need to add a "this"
        // Do this if current node is a field and we may replace a variable with
        // its name
        AST ast = node.getAST();
        IVariableBinding vBinding = (IVariableBinding) binding;
        // Check for field acceses
        if (vBinding.isField()) {
            if (renaming.containsValue(node.toString()) && !(node.getParent() instanceof FieldAccess)) {
                FieldAccess fa = ast.newFieldAccess();
                fa.setExpression(ast.newThisExpression());
                fa.setName(ast.newSimpleName(node.toString()));
                astRewrite.replace(node, fa, null);
            }
            return false;
        }
        String newName = renaming.get(node.toString());
        if (newName == null || newName == "")
            return false;
        astRewrite.replace(node, ast.newSimpleName(newName), null);
        return false;
    }
}
