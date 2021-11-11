/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.extension.apache.pdfbox.internal.operations;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.mule.extension.apache.pdfbox.internal.utils.ImageGraphicsEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public final class ExtractImages
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractImages.class);
    private int imageCounter = 1;

    public ExtractImages()
    {
    }

    public String ExtractImages(String pdfFile, String password, String prefix, boolean useDirectJPEG, boolean noColorConvert) throws IOException
    {
        // suppress the Dock icon on OS X
        System.setProperty("apple.awt.UIElement", "true");
        if (pdfFile == null)
        {
            LOGGER.debug("pdfFile is null");
            return usage();
        }
        else
        {
            if (prefix == null && pdfFile.length() >4)
            {
                prefix = pdfFile.substring(0, pdfFile.length() -4);
            }

            try (PDDocument document = Loader.loadPDF(new File(pdfFile), password))
            {
                AccessPermission ap = document.getCurrentAccessPermission();
                if (!ap.canExtractContent())
                {
                    throw new IOException("You do not have permission to extract images");
                }

                for (PDPage page : document.getPages())
                {
                    ImageGraphicsEngine extractor = new ImageGraphicsEngine(page);
                    extractor.run(prefix, imageCounter, useDirectJPEG,  noColorConvert);
                    imageCounter++;
                }
            }
        }
        return("The images from file " + pdfFile +" were extracted using the prefix " + prefix);
    }

    /**
     * Print the usage requirements and exit.
     */
    public static String usage()
    {
        String message = "Usage: " + ExtractImages.class.getName() + " [options] <inputfile>\n"
                + "\nOptions:\n"
                + "  -password <password>   : Password to decrypt document\n"
                + "  -prefix <image-prefix> : Image prefix (default to pdf name)\n"
                + "  -directJPEG            : Forces the direct extraction of JPEG/JPX images \n"
                + "                           regardless of colorspace or masking\n"
                + "  -noColorConvert        : Images are extracted with their \n"
                + "                           original colorspace if possible.\n"
                + "  <inputfile>            : The PDF document to use\n";

        return message;
    }
}
