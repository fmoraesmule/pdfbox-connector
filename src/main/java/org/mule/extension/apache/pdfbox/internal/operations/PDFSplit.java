package org.mule.extension.apache.pdfbox.internal.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;

public final class PDFSplit
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PDFSplit.class);

    public PDFSplit()
    {

    }

    public String PDFSplit( String inputFile, String outputPrefix, String startPage, String endPage, String split, String password) throws IOException
    {
        LOGGER.debug("startPage: " + startPage +
                    "\nendPage: " + endPage +
                    "\nsplit: " + split+
                    "\npassword: " + password
        );
        @SuppressWarnings({"squid:S2068"})
        Splitter splitter = new Splitter();

        System.setProperty("apple.awt.UIElement", "true");

        if( inputFile == null )
        {
           return usage();
        }
        else
        {
            if (outputPrefix == null)
            {
                outputPrefix = inputFile.substring(0, inputFile.lastIndexOf('.'));
            }
            PDDocument document = null;
            List<PDDocument> documents = null;
            try
            {
                document = Loader.loadPDF(new File(inputFile), password);

                int numberOfPages = document.getNumberOfPages();
                LOGGER.debug("numberOfPages: " + numberOfPages);
                boolean startEndPageSet = false;
                if (startPage != null)
                {
                    splitter.setStartPage(Integer.parseInt( startPage ));
                    startEndPageSet = true;
                    if (split == null)
                    {
                        splitter.setSplitAtPage(numberOfPages);
                    }
                }
                if (endPage != null)
                {
                    splitter.setEndPage(Integer.parseInt( endPage ));
                    startEndPageSet = true;
                    if (split == null)
                    {
                        splitter.setSplitAtPage(Integer.parseInt( endPage ));
                    }
                }
                if (split != null)
                {
                    splitter.setSplitAtPage( Integer.parseInt( split ) );
                }
                else
                {
                    if (!startEndPageSet)
                    {
                        splitter.setSplitAtPage(1);
                    }
                }

                documents = splitter.split( document );
                LOGGER.debug("documents.size(): " + documents.size());
                for( int i=0; i<documents.size(); i++ )
                {
                    try (PDDocument doc = documents.get(i))
                    {
                        doc.save(outputPrefix + "-" + (i + 1) + ".pdf");
                    }
                }

            }
            finally
            {
                LOGGER.debug("closing documents");
                if( document != null )
                {
                    document.close();
                }
                for( int i=0; documents != null && i<documents.size(); i++ )
                {
                    PDDocument doc = documents.get(i);
                    doc.close();
                }
            }
            return "File " + inputFile + " split";
        }
    }

    private static String usage()
    {
        String message = "Usage: PDFSplit [options] <inputfile>\n"
                + "\nOptions:\n"
                + "  -password  <password>  : Password to decrypt document\n"
                + "  -split     <integer>   : split after this many pages (default 1, if startPage and endPage are unset)\n"
                + "  -startPage <integer>   : start page\n"
                + "  -endPage   <integer>   : end page\n"
                + "  -outputPrefix <prefix> : Filename prefix for split files\n"
                + "  <inputfile>            : The PDF document to use\n";

        return message;
    }
}