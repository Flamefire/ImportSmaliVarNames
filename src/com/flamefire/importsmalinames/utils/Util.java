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

public abstract class Util {
    public static CompilationUnit createCU(ICompilationUnit cu, boolean resolveBindings) {
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setProject(cu.getJavaProject());
        parser.setSource(cu);
        parser.setResolveBindings(resolveBindings);
        parser.setStatementsRecovery(true);
        parser.setBindingsRecovery(true);
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
        if (str.endsWith(suffix))
            return str.substring(0, str.length() - suffix.length());
        return str;
    }
}
