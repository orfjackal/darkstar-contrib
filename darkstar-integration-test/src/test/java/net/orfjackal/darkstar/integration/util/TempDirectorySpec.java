/*
 * Copyright (c) 2008, Esko Luontola. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.orfjackal.darkstar.integration.util;

import jdave.Block;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

/**
 * @author Esko Luontola
 * @since 17.6.2008
 */
@RunWith(JDaveRunner.class)
public class TempDirectorySpec extends Specification<Object> {

    private static final File EXPECTED_DIR_1 = new File(System.getProperty("java.io.tmpdir"), TempDirectory.PREFIX + "1");
    private static final File EXPECTED_DIR_2 = new File(System.getProperty("java.io.tmpdir"), TempDirectory.PREFIX + "2");

    public void destroy() {
        EXPECTED_DIR_1.delete();
        EXPECTED_DIR_2.delete();
    }


    public class WhenATempDirectoryIsCreated {

        private TempDirectory tempDirectory;

        public Object create() {
            tempDirectory = new TempDirectory();
            return null;
        }

        public void atFirstTheDirectoryDoesNotExists() {
            specify(!EXPECTED_DIR_1.exists());
        }

        public void afterCreationTheDirectoryExists() {
            tempDirectory.create();
            specify(EXPECTED_DIR_1.exists());
            specify(tempDirectory.getDirectory(), should.equal(EXPECTED_DIR_1));
        }

        public void ifTheDirectoryAlreadyExistedADifferentNameIsUsed() {
            EXPECTED_DIR_1.mkdir();
            specify(EXPECTED_DIR_1.exists());
            specify(!EXPECTED_DIR_2.exists());
            tempDirectory.create();
            specify(EXPECTED_DIR_1.exists());
            specify(EXPECTED_DIR_2.exists());
            specify(tempDirectory.getDirectory(), should.equal(EXPECTED_DIR_2));
        }

        public void creatingTwiseIsNotAllowed() {
            tempDirectory.create();
            specify(new Block() {
                public void run() throws Throwable {
                    tempDirectory.create();
                }
            }, should.raise(IllegalArgumentException.class));
            specify(EXPECTED_DIR_1.exists());
            specify(!EXPECTED_DIR_2.exists());
        }
    }

    public class WhenATempDirectoryIsDisposed {

        private TempDirectory tempDirectory;
        private File directory;

        public Object create() {
            tempDirectory = new TempDirectory();
            tempDirectory.create();
            directory = tempDirectory.getDirectory();
            specify(directory.exists());
            specify(directory.equals(EXPECTED_DIR_1));
            return null;
        }

        public void theDirectoryIsDeleted() {
            tempDirectory.dispose();
            specify(!directory.exists());
        }

        public void allFilesInThatDirectoryAreDeleted() throws IOException {
            File f = new File(directory, "test.txt");
            f.createNewFile();
            specify(f.exists());
            tempDirectory.dispose();
            specify(!directory.exists());
        }

        public void allFilesInSubDirectoriesAreDeleted() throws IOException {
            File subdir = new File(directory, "subdir");
            subdir.mkdir();
            specify(subdir.exists());
            File f = new File(subdir, "test.txt");
            f.createNewFile();
            specify(f.exists());
            tempDirectory.dispose();
            specify(!directory.exists());
        }
    }

    public class WhenATempDirectoryIsCreatedWithAPredefinedPath {

        private File predefined;
        private TempDirectory tempDirectory;

        public Object create() {
            predefined = new File("foo.tmp");
            assert !predefined.exists();
            tempDirectory = new TempDirectory(predefined);
            return null;
        }

        public void destroy() {
            if (predefined.isDirectory()) {
                assert predefined.listFiles().length == 0;
                predefined.delete();
            }
        }

        public void theDirectoryMustNotAlreadyExist() throws IOException {
            predefined.mkdir();
            specify(new Block() {
                public void run() throws Throwable {
                    tempDirectory.create();
                }
            }, should.raise(IllegalArgumentException.class));
        }

        public void whenCreatedTheSpecifiedDirectoryExists() {
            specify(!predefined.exists());
            tempDirectory.create();
            specify(predefined.isDirectory());
            specify(tempDirectory.getDirectory(), should.equal(predefined));
        }

        public void whenDisposedTheSpecifiedDirectoryDoesNotExist() {
            tempDirectory.create();
            tempDirectory.dispose();
            specify(!predefined.exists());
        }

        public void itsParentDirectoryMustAlreadyExist() {
            File noParent = new File("foo.tmp", "bar.tmp");
            specify(!noParent.exists());
            specify(!noParent.getParentFile().exists());
            tempDirectory = new TempDirectory(noParent);
            specify(new Block() {
                public void run() throws Throwable {
                    tempDirectory.create();
                }
            }, should.raise(RuntimeException.class));
            specify(!noParent.exists());
        }
    }
}
