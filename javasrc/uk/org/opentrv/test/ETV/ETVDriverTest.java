package uk.org.opentrv.test.ETV;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.org.opentrv.ETV.driver.ETVSimpleDriverNBulkInputs;
import uk.org.opentrv.test.hdd.DDNExtractorTest;

public class ETVDriverTest
    {
    /**Private temp directory to do work in created for each test and cleared down after; never null during tests. */
    private Path tempDir;

    @Before
    public void before() throws Exception
        {
        tempDir = Files.createTempDirectory(null);
        }

    @After
    public void after() throws Exception
        {
        // Delete the directory tree.
        Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return(FileVisitResult.CONTINUE);
            }
            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                Files.delete(dir);
                return(FileVisitResult.CONTINUE);
            }
        });
        // Check that it has been cleared.
        if(tempDir.toFile().exists()) { throw new AssertionError("cleanup failed: " + tempDir); }
        tempDir = null;
        }

    /**Helper. */
    private static int cpReaderToWriter(final Reader input, final Writer output) throws IOException
        {
        final char buf[] = new char[8192];
        int len = 0;
        int n = 0;
        while(-1 != (n = input.read(buf)))
            {
            output.write(buf, 0, n);
            len += n;
            }
        return(len);
        }

    /**Test for correct processing of simple single-house data set. */
    @Test public void testWithSingleHouseDataSet() throws IOException
        {
        final File inDir = tempDir.toFile();
        final File outDir = tempDir.toFile();
        // Copy HDD file.
        try(final Reader r = DDNExtractorTest.getETVEGLLHDD2016H1CSVReader())
            {
            try(final FileWriter w = new FileWriter(new File(inDir, ETVSimpleDriverNBulkInputs.INPUT_FILE_HDD)))
                { cpReaderToWriter(r, w); }
            }
        // Copy sample household data file.
        try(final Reader r = ETVParseTest.getNBulkSH2016H1CSVReader())
            {
            try(final FileWriter w = new FileWriter(new File(inDir, ETVSimpleDriverNBulkInputs.INPUT_FILE_NKWH)))
                { cpReaderToWriter(r, w); }
            }
        // Do the computation...
        final File basicResultFile = new File(outDir, ETVSimpleDriverNBulkInputs.OUTPUT_FILE_BASIC_STATS);
        basicResultFile.delete(); // Make sure no output file.
        assertFalse("output file should not yet exist", basicResultFile.isFile());
        ETVSimpleDriverNBulkInputs.doComputation(inDir, outDir);
        assertTrue("output file should now exist", basicResultFile.isFile());
        final String expected =
            "\"house ID\",\"slope energy/HDD\",\"baseload energy\",\"R^2\",\"n\",\"efficiency gain if computed\"\n" +
            "\"5013\",1.5532478,1.3065631,0.62608224,156,\n";
        final String actual = new String(Files.readAllBytes(basicResultFile.toPath()), "ASCII7");
        assertEquals(expected, actual);
        }

    /**Input directory in home dir for testWithExternalDataSet(). */
    public static final String fixedDataSetDir = "ETV-prepared-data";
    /**Output directory in home dir for testWithExternalDataSet(). */
    public static final String fixedDataSetOutDir = "ETV-prepared-data-out";

    /**Test for robust driver behaviour on external data set if any.
     * Will not run if input data directory is missing.
     * <p>
     * Will fail for other errors.
     * <p>
     * Simply check that the output file gets generated.
     */
    @Test public void testWithExternalDataSet() throws IOException
        {
        final String homeDir = System.getProperty("user.home");
        assumeNotNull(null != homeDir);
        final File inDir = new File(new File(homeDir), fixedDataSetDir);
        assumeTrue(inDir.isDirectory());
        final File outDir = new File(new File(homeDir), fixedDataSetOutDir);
        assertTrue(outDir.isDirectory());
        assertTrue(outDir.canWrite());
        final File basicResultFile = new File(outDir, ETVSimpleDriverNBulkInputs.OUTPUT_FILE_BASIC_STATS);
        basicResultFile.delete(); // Make sure no output file.
        assertFalse("output file should not yet exist", basicResultFile.isFile());
        ETVSimpleDriverNBulkInputs.doComputation(inDir, outDir);
        assertTrue("output file should now exist", basicResultFile.isFile());
        }
    }
