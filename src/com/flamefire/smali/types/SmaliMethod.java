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

package com.flamefire.smali.types;

import com.flamefire.types.CMethod;

import java.util.List;

public class SmaliMethod extends CMethod<SmaliVariable> {

    public SmaliMethod(String name) {
        super(name);
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

    public void removeLeadingNullParams() {
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i) != null)
                break;
            parameters.remove(i);
            i--;
        }
    }

    public boolean containsNullParams() {
        for (SmaliVariable v : parameters)
            if (v == null)
                return true;
        return false;
    }
}
