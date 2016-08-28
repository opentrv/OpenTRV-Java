/*
The OpenTRV project licenses this file to you
under the Apache Licence, Version 2.0 (the "Licence");
you may not use this file except in compliance
with the Licence. You may obtain a copy of the Licence at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the Licence is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied. See the Licence for the
specific language governing permissions and limitations
under the Licence.

Author(s) / Copyright (s): Damon Hart-Davis 2016
*/

package uk.org.opentrv.test.ETV;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import uk.org.opentrv.ETV.filter.StatusSegmentation;

public class ETVSegmentationTest
    {
    /**Test for correct 'empty' segmentation. */
    @Test public void testEmptySegmentation() throws IOException
        {
        // Check that with no devices a non-null empty result is produced.
        assertNotNull(StatusSegmentation.segmentActivity(Collections.emptyList()));
        assertNotNull(StatusSegmentation.segmentActivity(Collections.emptyList()).getOptionalEnabledAndUsableFlagsByLocalDay());
        assertTrue(StatusSegmentation.segmentActivity(Collections.emptyList()).getOptionalEnabledAndUsableFlagsByLocalDay().isEmpty());
        }
    }
