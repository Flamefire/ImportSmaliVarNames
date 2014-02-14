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

package com.flamefire.importsmalinames.utils;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

public abstract class Util {
    public static CompilationUnit createCU(ICompilationUnit cu) {
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setSource(cu);
        parser.setResolveBindings(true);
        CompilationUnit unit = (CompilationUnit) parser.createAST(null);
        return unit;
    }

    public static String getPersistentProperty(IResource res, QualifiedName qn) {
        try {
            return res.getPersistentProperty(qn);
        } catch (CoreException e) {
            return "";
        }
    }

    public static void setPersistentProperty(IResource res, QualifiedName qn, String value) {
        try {
            res.setPersistentProperty(qn, value);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    public static void ASTExample(ICompilationUnit cu) {
        CompilationUnit cuNew = createCU(cu);
        ASTRewrite astRew = ASTRewrite.create(cuNew.getAST());
        cuNew.recordModifications();
        try {
            Document document = new Document(cu.getSource());
            // Map options = cu.getJavaProject().getOptions(true);
            TextEdit cuEdit = astRew.rewriteAST(document, null);
            cuEdit.apply(document);
            String newSource = document.get();
            cu = (ICompilationUnit) cuNew.getJavaElement();
            cu.getBuffer().setContents(newSource);
            /*
             * String name = cu.getElementName(); IFile file = (IFile)
             * cu.getResource(); TextFileChange change = new
             * TextFileChange(name, file); change.setTextType("java");
             * change.setEdit(edit); change.perform(new NullProgressMonitor());
             */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // removes the prefix from the string
    public static String removePrefix(String str, String prefix, int add) {
        if (str.startsWith(prefix))
            return str.substring(prefix.length() + add);
        return str;
    }

    public static String removePrefix(String str, String prefix) {
        return removePrefix(str, prefix, 0);
    }

    public static String removeSuffix(String str, String suffix) {
        if (str.endsWith("[]"))
            return str.substring(0, str.length() - suffix.length());
        return str;
    }
}
