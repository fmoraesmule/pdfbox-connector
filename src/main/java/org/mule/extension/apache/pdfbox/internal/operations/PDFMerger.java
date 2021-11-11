package org.mule.extension.apache.pdfbox.internal.operations;

import java.io.IOException;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PDFMerger
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PDFMerger.class);

    public PDFMerger()
    {
    }


    public String PDFMerger( String[] inputFiles,  String outputFile) throws IOException
    {
        System.setProperty("apple.awt.UIElement", "true");

        int firstFileArgPos = 0;

        if ( inputFiles.length - firstFileArgPos < 2 )
        {
            LOGGER.debug("Must set at least two files to merge");
            return usage();
        }

        PDFMergerUtility merger = new PDFMergerUtility();
        for( int i=firstFileArgPos; i<inputFiles.length; i++ )
        {
            String sourceFileName = inputFiles[i];
            merger.addSource(sourceFileName);
        }
        ;
        merger.setDestinationFileName(outputFile);
        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());

        return "Merged " + outputFile + " file";
    }

    private String usage()
    {
        String message = "Usage: PDFMerger "
                + "<inputfiles 2..n> <outputfile>\n"
                + "\nOptions:\n"
                + "  <inputfiles 2..n> : 2 or more source PDF documents to merge\n"
                + "  <outputfile>      : The PDF document to save the merged documents to\n";

        return message;
    }
}