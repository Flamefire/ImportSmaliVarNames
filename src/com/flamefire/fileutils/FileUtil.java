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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileUtil {
    public static final List<File> getFilesRecursive(final File basedir, final FileFilter filter) {
        List<File> files = new ArrayList<File>();
        if (basedir != null && basedir.isDirectory()) {
            for (File subdir : basedir.listFiles(TypeFilter.DIR)) {
                files.addAll(FileUtil.getFilesRecursive(subdir, filter));
            }

            files.addAll(Arrays.asList(basedir.listFiles(filter)));
        }

        return files;
    }
}
