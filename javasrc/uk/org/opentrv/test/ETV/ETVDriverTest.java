package uk.org.opentrv.test.ETV;

import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import uk.org.opentrv.ETV.driver.ETVSimpleDriverNBulkInputs;

public class ETVDriverTest
    {
    public static final String fixedDataSetDir = "ETV-prepared-data";
    public static final String fixedDataSetOutDir = "ETV-prepared-data-out";

    /**Test for robust driver behaviour on fixed data set if any.
     * Will not run if input data directory is missing.
     * Will fail for other errors.
     */
    @Test public void testWithExternalDataSet() throws IOException
        {
        final String homeDir = System.getProperty("user.home");
        assumeNotNull(null != homeDir);
        final File inDir = new File(new File(homeDir), fixedDataSetDir);
        assumeTrue(inDir.isDirectory());
        final File outDir = new File(new File(homeDir), fixedDataSetOutDir);
//        ETVSimpleDriverNBulkInputs.doComputation(inDir, outDir);

//        final List<ETVPerHouseholdComputationResult> rl = Arrays.asList(r1, r2);
//        final String rlCSV = (new ETVPerHouseholdComputationResultsToCSV()).apply(rl);
//      System.out.println(rlCSV);
//        assertEquals(
//              "\"house ID\",\"slope energy/HDD\",\"baseload energy\",\"R^2\",\"n\",\"efficiency gain if computed\"\n" +
//              "\"1234\",1.2,5.4,0.8,63,\n" +
//              "\"56\",7.89,0.1,0.6,532,1.23\n",
//              rlCSV);

        // TODO


        }
    }
