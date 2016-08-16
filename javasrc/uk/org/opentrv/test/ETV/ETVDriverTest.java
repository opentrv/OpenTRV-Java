package uk.org.opentrv.test.ETV;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import uk.org.opentrv.ETV.driver.ETVSimpleDriverNBulkInputs;

public class ETVDriverTest
    {
    /**Input directory in home dir for testWithExternalDataSet(). */
    public static final String fixedDataSetDir = "ETV-prepared-data";
    /**Output directory in home dir for testWithExternalDataSet(). */
    public static final String fixedDataSetOutDir = "ETV-prepared-data-out";

    /**Test for robust driver behaviour on fixed data set if any.
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
