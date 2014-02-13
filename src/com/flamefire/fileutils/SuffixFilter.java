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
package com.flamefire.fileutils;


import java.util.regex.Pattern;

/*
 * SuffixFilter.java accepts files that match a given suffix and type
 */
public class SuffixFilter extends RegExFilter {
    public SuffixFilter(final TypeFilter type, final String suffix) {
        super(type, "^.*" + Pattern.quote(suffix) + "$");
    }
}
