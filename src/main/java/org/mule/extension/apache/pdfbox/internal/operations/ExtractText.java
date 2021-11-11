package org.mule.extension.apache.pdfbox.internal.operations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.mule.extension.apache.pdfbox.internal.utils.PDFText2HTML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class ExtractText
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractText.class);
    private static final String STD_ENCODING = "UTF-8";

    public ExtractText() throws IOException {
        //static class
    }


    public String ExtractText(String pdfFile, String outputFile, String password, boolean toConsole, boolean toHTML, boolean sort, boolean separateBeads,  boolean alwaysNext, boolean rotationMagic,
                                String startPage, String endPage) throws IOException
    {
        System.setProperty("apple.awt.UIElement", "true");
        @SuppressWarnings({"squid:S2068"})
        String encoding = STD_ENCODING;
        // Defaults to text files
        String ext = ".txt";

        if( pdfFile == null )
        {
            return usage();
        }
        else
        {

            Writer output = null;
            PDDocument document = null;
            try
            {
                long startTime = startProcessing("Loading PDF "+pdfFile);
                if( outputFile == null && pdfFile.length() >4 )
                {
                    outputFile = new File( pdfFile.substring( 0, pdfFile.length() -4 ) + ext ).getAbsolutePath();
                }
                document = Loader.loadPDF(new File(pdfFile), password);

                AccessPermission ap = document.getCurrentAccessPermission();
                if( ! ap.canExtractContent() )
                {
                    throw new IOException( "You do not have permission to extract text" );
                }

                stopProcessing("Time for loading: ", startTime);

                if( toConsole )
                {
                    output = new OutputStreamWriter( System.out, encoding );
                }
                else
                {
                    if (toHTML && !STD_ENCODING.equals(encoding))
                    {
                        encoding = STD_ENCODING;
                        LOGGER.debug("The encoding parameter is ignored when writing html output.");
                    }
                    output = new OutputStreamWriter( new FileOutputStream( outputFile ), encoding );
                }
                startTime = startProcessing("Starting text extraction");

                LOGGER.debug("Writing to " + outputFile);

                PDFTextStripper stripper;
                if(toHTML)
                {
                    // HTML stripper can't work page by page because of startDocument() callback
                    stripper = new PDFText2HTML();
                    stripper.setSortByPosition(sort);
                    stripper.setShouldSeparateByBeads(separateBeads);
                    stripper.setStartPage(Integer.parseInt(startPage));
                    stripper.setEndPage(Integer.parseInt(endPage));

                    // Extract text for main document:
                    stripper.writeText(document, output);
                }
                else
                {
                    if (rotationMagic)
                    {
                        stripper = new FilteredTextStripper();
                    }
                    else
                    {
                        stripper = new PDFTextStripper();
                    }
                    stripper.setSortByPosition(sort);
                    stripper.setShouldSeparateByBeads(separateBeads);

                    // Extract text for main document:
                    extractPages(Integer.parseInt(startPage), Math.min(Integer.parseInt(endPage), document.getNumberOfPages()),
                            stripper, document, output, rotationMagic, alwaysNext);
                }

                // ... also for any embedded PDFs:
                PDDocumentCatalog catalog = document.getDocumentCatalog();
                PDDocumentNameDictionary names = catalog.getNames();
                if (names != null)
                {
                    PDEmbeddedFilesNameTreeNode embeddedFiles = names.getEmbeddedFiles();
                    if (embeddedFiles != null)
                    {
                        Map<String, PDComplexFileSpecification> embeddedFileNames = embeddedFiles.getNames();
                        if (embeddedFileNames != null)
                        {
                            for (Map.Entry<String, PDComplexFileSpecification> ent : embeddedFileNames.entrySet())
                            {
                                LOGGER.debug("Processing embedded file " + ent.getKey() + ":");

                                PDComplexFileSpecification spec = ent.getValue();
                                PDEmbeddedFile file = spec.getEmbeddedFile();
                                if (file != null && "application/pdf".equals(file.getSubtype()))
                                {
                                    LOGGER.debug("  is PDF (size=" + file.getSize() + ")");

                                    try (InputStream fis = file.createInputStream();
                                         PDDocument subDoc = Loader.loadPDF(fis))
                                    {
                                        if (toHTML)
                                        {
                                            // will not really work because of HTML header + footer
                                            stripper.writeText( subDoc, output );
                                        }
                                        else
                                        {
                                            extractPages(1, subDoc.getNumberOfPages(),
                                                    stripper, subDoc, output, rotationMagic, alwaysNext);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                stopProcessing("Time for extraction: ", startTime);
            }
            finally
            {
                IOUtils.closeQuietly(output);
                IOUtils.closeQuietly(document);
                return "Text extracted to " + outputFile;
            }
        }
    }

    private void extractPages(int startPage, int endPage,
                              PDFTextStripper stripper, PDDocument document, Writer output,
                              boolean rotationMagic, boolean alwaysNext) throws IOException
    {
        for (int p = startPage; p <= endPage; ++p)
        {
            stripper.setStartPage(p);
            stripper.setEndPage(p);
            try
            {
                if (rotationMagic)
                {
                    PDPage page = document.getPage(p - 1);
                    int rotation = page.getRotation();
                    page.setRotation(0);
                    AngleCollector angleCollector = new AngleCollector();
                    angleCollector.setStartPage(p);
                    angleCollector.setEndPage(p);
                    angleCollector.writeText(document, new NullWriter());
                    // rotation magic
                    for (int angle : angleCollector.getAngles())
                    {
                        // prepend a transformation
                        // (we could skip these parts for angle 0, but it doesn't matter much)
                        try (PDPageContentStream cs = new PDPageContentStream(document, page,
                                PDPageContentStream.AppendMode.PREPEND, false))
                        {
                            cs.transform(Matrix.getRotateInstance(-Math.toRadians(angle), 0, 0));
                        }

                        stripper.writeText(document, output);

                        // remove prepended transformation
                        ((COSArray) page.getCOSObject().getItem(COSName.CONTENTS)).remove(0);
                    }
                    page.setRotation(rotation);
                }
                else
                {
                    stripper.writeText(document, output);
                }
            }
            catch (IOException ex)
            {
                if (!alwaysNext)
                {
                    throw ex;
                }
                LOGGER.error("Failed to process page " + p, ex);
            }
        }
    }

    private long startProcessing(String message)
    {
        LOGGER.debug(message);
        return System.currentTimeMillis();
    }

    private void stopProcessing(String message, long startTime)
    {

        long stopTime = System.currentTimeMillis();
        float elapsedTime = ((float)(stopTime - startTime))/1000;
        LOGGER.debug(message + elapsedTime + " seconds");
    }

    static int getAngle(TextPosition text)
    {
        // should this become a part of TextPosition?
        Matrix m = text.getTextMatrix().clone();
        m.concatenate(text.getFont().getFontMatrix());
        return (int) Math.round(Math.toDegrees(Math.atan2(m.getShearY(), m.getScaleY())));
    }

    private String usage()
    {
        String message = "Usage: ExtractText [options] <inputfile> [output-text-file]\n"
                + "\nOptions:\n"
                + "  -password <password>        : Password to decrypt document\n"
                + "  -encoding <output encoding> : UTF-8 (default) or ISO-8859-1, UTF-16BE,\n"
                + "                                UTF-16LE, etc.\n"
                + "  -console                    : Send text to console instead of file\n"
                + "  -html                       : Output in HTML format instead of raw text\n"
                + "  -sort                       : Sort the text before writing\n"
                + "  -ignoreBeads                : Disables the separation by beads\n"
                + "  -debug                      : Enables debug output about the time consumption\n"
                + "                                of every stage\n"
                + "  -alwaysNext                 : Process next page (if applicable) despite\n"
                + "                                IOException (ignored when -html)\n"
                + "  -rotationMagic              : Analyze each page for rotated/skewed text,\n"
                + "                                rotate to 0° and extract separately\n"
                + "                                (slower, and ignored when -html)\n"
                + "  -startPage <number>         : The first page to start extraction (1 based)\n"
                + "  -endPage <number>           : The last page to extract (1 based, inclusive)\n"
                + "  <inputfile>                 : The PDF document to use\n"
                + "  [output-text-file]          : The file to write the text to";

        return message;
    }
}

/**
 * Collect all angles while doing text extraction. Angles are in degrees and rounded to the closest
 * integer (to avoid slight differences from floating point arithmetic resulting in similarly
 * angled glyphs being treated separately). This class must be constructed for each page so that the
 * angle set is initialized.
 */
class AngleCollector extends PDFTextStripper
{
    private final Set<Integer> angles = new TreeSet<>();

    AngleCollector() throws IOException
    {
    }

    Set<Integer> getAngles()
    {
        return angles;
    }

    @Override
    protected void processTextPosition(TextPosition text)
    {
        int angle = ExtractText.getAngle(text);
        angle = (angle + 360) % 360;
        angles.add(angle);
    }
}

/**
 * TextStripper that only processes glyphs that have angle 0.
 */
class FilteredTextStripper extends PDFTextStripper
{
    FilteredTextStripper() throws IOException
    {
    }

    @Override
    protected void processTextPosition(TextPosition text)
    {
        int angle = ExtractText.getAngle(text);
        if (angle == 0)
        {
            super.processTextPosition(text);
        }
    }
}

/**
 * Dummy output.
 */
class NullWriter extends Writer
{
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException
    {
        // do nothing
    }

    @Override
    public void flush() throws IOException
    {
        // do nothing
    }

    @Override
    public void close() throws IOException
    {
        // do nothing
    }
}
