package org.mule.extension.apache.pdfbox.internal.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.multipdf.Overlay.Position;
import org.apache.pdfbox.pdmodel.PDDocument;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import static org.mule.runtime.extension.api.annotation.param.MediaType.*;
import org.mule.extension.apache.pdfbox.api.enums.PagesBehavior;
import org.mule.extension.apache.pdfbox.api.enums.OverlayPosition;

@Alias("OverlayPDF")
@EmitsResponse
@MediaType(value = ANY, strict = false)
public class OverlayPDF{
    private static final Logger LOGGER = LoggerFactory.getLogger(OverlayPDF.class);

    public OverlayPDF(){}

    public String OverlayPDF(String inputFile, String outputFile, String overlayFile, OverlayPosition position, PagesBehavior pagesBehavior, String page) throws IOException{
        // suppress the Dock icon on OS X
        System.setProperty("apple.awt.UIElement", "true");

        LOGGER.debug("Input file: " + inputFile + " \noutputFile: " + outputFile
                     + "\noverlayFile: "+ overlayFile
                     + "\nposition: "+ position.getPosition()
                     + "\npage behavior: " + pagesBehavior.getPagesBehavior()
                     + "\npage: " + page);

        Overlay overlayer = new Overlay();
        Map<Integer, String> specificPageOverlayFile = new HashMap<>();

        if (inputFile != null)
        {
            overlayer.setInputFile(inputFile);
        }
        if (position.getPosition() != null)
        {
            if (Position.FOREGROUND.toString().equalsIgnoreCase(position.getPosition()))
            {
                overlayer.setOverlayPosition(Position.FOREGROUND);
            }
            else if (Position.BACKGROUND.toString().equalsIgnoreCase(position.getPosition()))
            {
                overlayer.setOverlayPosition(Position.BACKGROUND);
            }
            else
            {
                return usage();
            }
        }

        if (pagesBehavior.getPagesBehavior() == "ODD")
        {
            overlayer.setOddPageOverlayFile(overlayFile);
        }
        else if (pagesBehavior.getPagesBehavior() == "EVEN")
        {
            overlayer.setEvenPageOverlayFile(overlayFile);
        }
        else if (pagesBehavior.getPagesBehavior() == "FIRST")
        {
            overlayer.setFirstPageOverlayFile(overlayFile);
        }
        else if (pagesBehavior.getPagesBehavior() == "LAST")
        {
            overlayer.setLastPageOverlayFile(overlayFile);
        }
        else if (pagesBehavior.getPagesBehavior().equals("ALL"))
        {
            overlayer.setAllPagesOverlayFile(overlayFile);
        }
        else if (page != null)
        {
            specificPageOverlayFile.put(Integer.parseInt(page), overlayFile);
        }

        if (overlayer.getDefaultOverlayFile() == null)
        {
            overlayer.setDefaultOverlayFile(null);
        }
        else
        {
            return usage();
        }

        if (overlayer.getInputFile() == null || outputFile == null)
        {
            usage();
        }

        try (PDDocument result = overlayer.overlay(specificPageOverlayFile))
        {
            result.save(outputFile);
        }
        catch (IOException e)
        {
            LOGGER.error("Overlay failed: " + e.getMessage(), e);
            throw e;
        }
        finally
        {
            overlayer.close();
        }

        return "Saved " + outputFile + " file";
    }

    private static String usage()
    {
        String message = "Usage: OverlayPDF <inputfile> [options] <outputfile>\n"
                + "\nOptions:\n"
                + "  <inputfile>                                  : input file\n"
                + "  <defaultOverlay.pdf>                         : default overlay file\n"
                + "  -odd <oddPageOverlay.pdf>                    : overlay file used for odd pages\n"
                + "  -even <evenPageOverlay.pdf>                  : overlay file used for even pages\n"
                + "  -first <firstPageOverlay.pdf>                : overlay file used for the first page\n"
                + "  -last <lastPageOverlay.pdf>                  : overlay file used for the last page\n"
                + "  -useAllPages <allPagesOverlay.pdf>           : overlay file used for overlay, all pages"
                + " are used by simply repeating them\n"
                + "  -page <pageNumber> <specificPageOverlay.pdf> : overlay file used for "
                + "the given page number, may occur more than once\n"
                + "  -position foreground|background              : where to put the overlay "
                + "file: foreground or background\n"
                + "  <outputfile>                                 : output file";
        return  message;
    }

}