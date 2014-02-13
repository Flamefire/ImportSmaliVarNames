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
package com.flamefire.smali.types;

import java.util.ArrayList;
import java.util.List;

public class SmaliClass {
    public final List<SmaliMethod> methods = new ArrayList<SmaliMethod>();
    public final String name;
    public String finalName;

    public SmaliClass(String name) {
        this.name = name;
        finalName = name;
    }

    @Override
    public String toString() {
        String result = finalName + "{\n";
        for (SmaliMethod m : methods) {
            result += "\t" + m + "\n";
        }
        result += "}";
        return result;
    }

    public List<SmaliMethod> getMethods(String mName, int paramCount) {
        List<SmaliMethod> result = new ArrayList<SmaliMethod>();
        for (SmaliMethod m : methods) {
            if (m.name.equals(mName) && m.parameters.size() == paramCount)
                result.add(m);
        }
        return result;
    }
}
