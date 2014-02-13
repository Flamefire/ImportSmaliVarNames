/*******************************************************************************
 * Copyright (C) 2014 Flamefire
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.flamefire.renamevariables.handlers;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ClassGatherer {
    private final Map<String, IType> classes = new HashMap<String, IType>();
    private String pck;

    public Map<String, IType> getClasses(IJavaElement el) {
        classes.clear();
        pck = "";
        gather(el, "", new AtomicReference<Integer>(0));
        return classes;
    }

    private void gather(IJavaElement el, String parent, AtomicReference<Integer> anonCount) {
        try {
            if (el instanceof IPackageDeclaration)
                pck = el.getElementName() + ".";
            else if (el instanceof IType) {
                String name = el.getElementName();
                if (name.equals("")) {
                    anonCount.set(anonCount.get() + 1);
                    name = "$" + anonCount;
                } else if (!parent.equals(""))
                    parent += ".";
                parent += name;
                classes.put(pck + parent, (IType) el);
            } else if (el instanceof IMethod) {
                if (!parent.equals(""))
                    parent += "->";
                parent += el.getElementName();
            }
            if (el instanceof IParent) {
                AtomicReference<Integer> nAnonCount = new AtomicReference<Integer>(0);
                for (IJavaElement e : ((IParent) el).getChildren())
                    gather(e, parent, nAnonCount);
            }
        } catch (JavaModelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
