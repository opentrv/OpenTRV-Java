package uk.org.opentrv.test.ETV;

import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class ETVDriverTest
    {
    public static final String fixedDataSetDir = "ETV-prepared-data";
    public static final String fixedDataSetOutDir = "ETV-prepared-data-out";

    /**Test for correct driver behaviour on fixed data set if any. */
    @Test public void testFixedDataSet() throws IOException
        {
        final String homeDir = System.getProperty("user.home");
        assumeNotNull(null != homeDir);
        final File inDir = new File(new File(homeDir), fixedDataSetDir);
        assumeTrue(inDir.isDirectory());
        final File outDir = new File(new File(homeDir), fixedDataSetOutDir);
//        ETVSimpleDriverNBulkInputs.doComputation(inDir, outDir);



        // TODO


        }

    }
