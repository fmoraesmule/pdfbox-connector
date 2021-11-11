package org.mule.extension.apache.pdfbox.internal.operations;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.mule.extension.apache.pdfbox.api.exceptions.InvalidColorException;
import org.mule.extension.apache.pdfbox.api.exceptions.NoWriterFoundException;
import org.mule.extension.apache.pdfbox.internal.utils.imageio.imageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public final class PDFToImage
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PDFToImage.class);

    public PDFToImage()
    {
    }

    public String PDFToImage(String pdfFile, String outputPrefix, int startPage, int endPage, String password,
                            String imageFormat, String color, int dpi, float quality, float cropBoxLowerLeftX,
                            float cropBoxLowerLeftY, float cropBoxUpperRightX, float cropBoxUpperRightY, boolean subsampling) throws IOException, NoWriterFoundException, InvalidColorException {
        // suppress the Dock icon on OS X
        System.setProperty("apple.awt.UIElement", "true");
        String fileName = new String();
        String message = new String();
        if(endPage==0){
            endPage = Integer.MAX_VALUE;
        }
        try
        {
            dpi = Toolkit.getDefaultToolkit().getScreenResolution();
        }
        catch( HeadlessException e )
        {
            dpi = 96;
        }

        if( pdfFile == null )
        {
            return usage();
        }
        else
        {
            if(outputPrefix == null)
            {
                outputPrefix = pdfFile.substring( 0, pdfFile.lastIndexOf( '.' ));
            }
            if (quality < 0)
            {
                quality = "png".equals(imageFormat) ? 0f : 1f;
            }

            try (PDDocument document = Loader.loadPDF(new File(pdfFile), password))
            {
                PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
                if (acroForm != null && acroForm.getNeedAppearances())
                {
                    acroForm.refreshAppearances();
                }
                ImageType imageType = null;
                if ("bilevel".equalsIgnoreCase(color))
                {
                    imageType = ImageType.BINARY;
                }
                else if ("gray".equalsIgnoreCase(color))
                {
                    imageType = ImageType.GRAY;
                }
                else if ("rgb".equalsIgnoreCase(color))
                {
                    imageType = ImageType.RGB;
                }
                else if ("rgba".equalsIgnoreCase(color))
                {
                    imageType = ImageType.ARGB;
                }

                if (imageType == null)
                {
                    LOGGER.error( "Error: Invalid color." );
                    throw new InvalidColorException("Invalid color " + imageType);
                }

                //if a CropBox has been specified, update the CropBox:
                //changeCropBoxes(PDDocument document,float a, float b, float c,float d)
                if (Float.compare(cropBoxLowerLeftX, 0) !=0 ||
                        Float.compare(cropBoxLowerLeftY, 0) !=0 ||
                        Float.compare(cropBoxUpperRightX, 0) !=0 ||
                        Float.compare(cropBoxUpperRightY, 0) !=0 )
                {
                    changeCropBox(document,
                            cropBoxLowerLeftX, cropBoxLowerLeftY,
                            cropBoxUpperRightX, cropBoxUpperRightY);
                }

                long startTime = System.nanoTime();

                // render the pages
                boolean success = true;
                endPage = Math.min(endPage, document.getNumberOfPages());
                PDFRenderer renderer = new PDFRenderer(document);
                renderer.setSubsamplingAllowed(subsampling);
                for (int i = startPage - 1; i < endPage; i++)
                {
                    LOGGER.debug("renderer.renderImageWithDPI("+i+", "+dpi+", "+imageType+")");
                    BufferedImage image = renderer.renderImageWithDPI(i, dpi, imageType);
                    fileName = outputPrefix + (i + 1) + "." + imageFormat;
                    success &= imageIOUtil.writeImage(image, fileName, dpi, quality);
                    message = message + "\n file" + pdfFile + " exported to image " + fileName;
                }

                // performance stats
                long endTime = System.nanoTime();
                long duration = endTime - startTime;
                int count = 1 + endPage - startPage;
                LOGGER.debug("Rendered %d page%s in %dms%n", count, count == 1 ? "" : "s", duration / 1000000);
                if (!success)
                {
                    LOGGER.error( "Error: no writer found for image format '"
                            + imageFormat + "'" );
                    throw new NoWriterFoundException("No writer found for image format '" + imageFormat + "'");
                }
            }
        }
        return message;
    }

    /**
     * This will print the usage requirements and exit.
     */
    public String usage()
    {
        String message = "Usage: PDFToImage [options] <inputfile>\n"
                + "\nOptions:\n"
                + "  -password  <password>            : Password to decrypt document\n"
                + "  -format <string>                 : Available image formats: " + getImageFormats() + "\n"
                + "  -prefix <string>                 : Filename prefix for image files\n"
                + "  -page <int>                      : The only page to extract (1-based)\n"
                + "  -startPage <int>                 : The first page to start extraction (1-based)\n"
                + "  -endPage <int>                   : The last page to extract (inclusive)\n"
                + "  -color <string>                  : The color depth (valid: bilevel, gray, rgb (default), rgba)\n"
                + "  -dpi <int>                       : The DPI of the output image, default: screen resolution or 96 if unknown\n"
                + "  -quality <float>                 : The quality to be used when compressing the image (0 <= quality <= 1)\n"
                + "                                     (default: 0 for PNG and 1 for the other formats)\n"
                + "  -cropbox <int> <int> <int> <int> : The page area to export\n"
                + "  -time                            : Prints timing information to stdout\n"
                + "  -subsampling                     : Activate subsampling (for PDFs with huge images)\n"
                + "  <inputfile>                      : The PDF document to use\n";

        return message;
    }

    private static String getImageFormats()
    {
        StringBuilder retval = new StringBuilder();
        String[] formats = ImageIO.getWriterFormatNames();
        for( int i = 0; i < formats.length; i++ )
        {
            if (formats[i].equalsIgnoreCase(formats[i]))
            {
                retval.append( formats[i] );
                if( i + 1 < formats.length )
                {
                    retval.append( ", " );
                }
            }
        }
        return retval.toString();
    }

    private static void changeCropBox(PDDocument document, float a, float b, float c, float d)
    {
        for (PDPage page : document.getPages())
        {
            LOGGER.debug("resizing page");
            PDRectangle rectangle = new PDRectangle();
            rectangle.setLowerLeftX(a);
            rectangle.setLowerLeftY(b);
            rectangle.setUpperRightX(c);
            rectangle.setUpperRightY(d);
            page.setCropBox(rectangle);
        }
    }
}
