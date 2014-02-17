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

public class CClass<T extends CMethod<U>, U extends CVariable> {
    public final List<T> methods = new ArrayList<T>();
    public String name;

    public CClass(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        String result = name + "{\n";
        for (T m : methods) {
            result += "\t" + m + "\n";
        }
        result += "}";
        return result;
    }

    public List<T> getMethods(String mName, int paramCount) {
        List<T> result = new ArrayList<T>();
        for (T m : methods) {
            if (m.name.equals(mName) && m.parameters.size() == paramCount)
                result.add(m);
        }
        return result;
    }

    public boolean hasDefaultConstructor() {
        for (T m : methods) {
            if (m.isDefaultConstructor())
                return true;
        }
        return false;
    }
}
