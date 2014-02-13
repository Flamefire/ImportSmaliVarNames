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

public class SmaliMethod {
    public final List<SmaliVariable> parameters = new ArrayList<SmaliVariable>();
    public final List<SmaliVariable> variables = new ArrayList<SmaliVariable>();
    public final String name;

    public SmaliMethod(String name) {
        this.name = name;
    }

    private void cleanUpLongVars(List<SmaliVariable> vars) {
        for (int i = 0; i < vars.size(); i++) {
            if (vars.get(i) != null && vars.get(i).isLong() && i + 1 < vars.size()) {
                assert (vars.get(i + 1) == null);
                vars.remove(i + 1);
            }
        }
    }

    public void cleanUpVars() {
        cleanUpLongVars(parameters);
    }

    @Override
    public String toString() {
        String result = name + "(";
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0)
                result += ", ";
            result += parameters.get(i);
        }
        result += "): ";
        for (int i = 0; i < variables.size(); i++) {
            if (i > 0)
                result += ", ";
            result += variables.get(i);
        }
        return result;
    }
}
