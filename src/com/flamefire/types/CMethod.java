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

package com.flamefire.types;

import java.util.ArrayList;
import java.util.List;

public class CMethod<T extends CVariable> {
    public static final String CONSTRUCTOR = "<init>";
    public final List<T> parameters = new ArrayList<T>();
    public final List<T> variables = new ArrayList<T>();
    public final String name;
    public final boolean isAbstract;

    public CMethod(String name, boolean isAbstract) {
        this.name = name;
        this.isAbstract = isAbstract;
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

    public int getNumberOfParameters() {
        return parameters.size();
    }

    public int getNumberOfLocalVariables() {
        return variables.size();
    }

    public boolean isConstructor() {
        return name.equals(CONSTRUCTOR);
    }

    public boolean isDefaultConstructor() {
        return isConstructor() && parameters.size() == 0;
    }
}
