package org.mule.extension.apache.pdfbox.internal.utils;

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.color.PDPattern;
import org.apache.pdfbox.pdmodel.graphics.form.PDTransparencyGroup;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.PDSoftMask;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.mule.extension.apache.pdfbox.internal.operations.ExtractImages;
import org.mule.extension.apache.pdfbox.internal.utils.imageio.imageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ImageGraphicsEngine extends PDFGraphicsStreamEngine
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractImages.class);
    private static final List<String> JPEG = Arrays.asList(
            COSName.DCT_DECODE.getName(),
            COSName.DCT_DECODE_ABBREVIATION.getName());

    private final Set<COSStream> seen = new HashSet<>();
    private String prefix = null;
    private int imageCounter;
    boolean useDirectJPEG, noColorConvert = false;
    public ImageGraphicsEngine(PDPage page)
    {
        super(page);
    }

    public void run(String prefix, int imageCounter, boolean useDirectJPEG, boolean noColorConvert) throws IOException
    {
        this.prefix = prefix;
        this.imageCounter = imageCounter;
        this.useDirectJPEG = useDirectJPEG;
        this.noColorConvert = noColorConvert;
        PDPage page = getPage();
        processPage(page);
        PDResources res = page.getResources();
        if (res == null)
        {
            LOGGER.debug("resources not found. PDPage.getResources() is null.");
            return;
        }
        for (COSName name : res.getExtGStateNames())
        {
            PDExtendedGraphicsState extGState = res.getExtGState(name);
            if (extGState == null)
            {
                // can happen if key exists but no value
                continue;
            }
            PDSoftMask softMask = extGState.getSoftMask();
            if (softMask != null)
            {
                PDTransparencyGroup group = softMask.getGroup();
                if (group != null)
                {
                    // PDFBOX-4327: without this line NPEs will occur
                    res.getExtGState(name).copyIntoGraphicsState(getGraphicsState());
                    processSoftMask(group);
                }
            }
        }
    }

    @Override
    public void drawImage(PDImage pdImage) throws IOException
    {
        if (pdImage instanceof PDImageXObject)
        {
            if (pdImage.isStencil())
            {
                processColor(getGraphicsState().getNonStrokingColor());
            }
            PDImageXObject xobject = (PDImageXObject)pdImage;
            if (seen.contains(xobject.getCOSObject()))
            {
                // skip duplicate image
                return;
            }
            seen.add(xobject.getCOSObject());
        }

        // save image
        String name = prefix + "-" + imageCounter;

        LOGGER.debug("Writing image: " + name);
        write2file(pdImage, name, useDirectJPEG, noColorConvert);
    }

    @Override
    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3)
            throws IOException
    {

    }

    @Override
    public void clip(int windingRule) throws IOException
    {

    }

    @Override
    public void moveTo(float x, float y) throws IOException
    {

    }

    @Override
    public void lineTo(float x, float y) throws IOException
    {

    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3)
            throws IOException
    {

    }

    @Override
    public Point2D getCurrentPoint() throws IOException
    {
        return new Point2D.Float(0, 0);
    }

    @Override
    public void closePath() throws IOException
    {

    }

    @Override
    public void endPath() throws IOException
    {

    }

    @Override
    protected void showGlyph(Matrix textRenderingMatrix,
                             PDFont font,
                             int code,
                             Vector displacement) throws IOException
    {
        RenderingMode renderingMode = getGraphicsState().getTextState().getRenderingMode();
        if (renderingMode.isFill())
        {
            processColor(getGraphicsState().getNonStrokingColor());
        }
        if (renderingMode.isStroke())
        {
            processColor(getGraphicsState().getStrokingColor());
        }
    }

    @Override
    public void strokePath() throws IOException
    {
        processColor(getGraphicsState().getStrokingColor());
    }

    @Override
    public void fillPath(int windingRule) throws IOException
    {
        processColor(getGraphicsState().getNonStrokingColor());
    }

    @Override
    public void fillAndStrokePath(int windingRule) throws IOException
    {
        processColor(getGraphicsState().getNonStrokingColor());
    }

    @Override
    public void shadingFill(COSName shadingName) throws IOException
    {

    }

    // find out if it is a tiling pattern, then process that one
    private void processColor(PDColor color) throws IOException
    {
        if (color.getColorSpace() instanceof PDPattern)
        {
            PDPattern pattern = (PDPattern) color.getColorSpace();
            PDAbstractPattern abstractPattern = pattern.getPattern(color);
            if (abstractPattern instanceof PDTilingPattern)
            {
                processTilingPattern((PDTilingPattern) abstractPattern, null, null);
            }
        }
    }

    /**
     * Writes the image to a file with the filename prefix + an appropriate suffix, like
     * "Image.jpg". The suffix is automatically set depending on the image compression in the
     * PDF.
     *
     * @param pdImage the image.
     * @param prefix the filename prefix.
     * @param directJPEG if true, force saving JPEG/JPX streams as they are in the PDF file.
     * @param noColorConvert if true, images are extracted with their original colorspace if
     * possible.
     * @throws IOException When something is wrong with the corresponding file.
     */
    private void write2file(PDImage pdImage, String prefix, boolean directJPEG,
                            boolean noColorConvert) throws IOException
    {
        String suffix = pdImage.getSuffix();
        if (suffix == null || "jb2".equals(suffix))
        {
            suffix = "png";
        }
        else if ("jpx".equals(suffix))
        {
            // use jp2 suffix for file because jpx not known by windows
            suffix = "jp2";
        }

        if (hasMasks(pdImage))
        {
            // TIKA-3040, PDFBOX-4771: can't save ARGB as JPEG
            suffix = "png";
        }

        if (noColorConvert)
        {
            // We write the raw image if in any way possible.
            // But we have no alpha information here.
            BufferedImage image = pdImage.getRawImage();
            if (image != null)
            {
                int elements = image.getRaster().getNumDataElements();
                suffix = "png";
                if (elements > 3)
                {
                    // More then 3 channels: Thats likely CMYK. We use tiff here,
                    // but a TIFF codec must be in the class path for this to work.
                    suffix = "tiff";
                }
                try (FileOutputStream out = new FileOutputStream(prefix + "." + suffix))
                {
                    imageIOUtil.writeImage(image, suffix, out);
                    out.flush();
                }
                return;
            }
        }
        try (FileOutputStream out = new FileOutputStream(prefix + "." + suffix))
        {
            if ("jpg".equals(suffix))
            {
                String colorSpaceName = pdImage.getColorSpace().getName();
                if (directJPEG ||
                        (PDDeviceGray.INSTANCE.getName().equals(colorSpaceName) ||
                                PDDeviceRGB.INSTANCE.getName().equals(colorSpaceName)))
                {
                    // RGB or Gray colorspace: get and write the unmodified JPEG stream
                    InputStream data = pdImage.createInputStream(JPEG);
                    IOUtils.copy(data, out);
                    IOUtils.closeQuietly(data);
                }
                else
                {
                    // for CMYK and other "unusual" colorspaces, the JPEG will be converted
                    BufferedImage image = pdImage.getImage();
                    if (image != null)
                    {
                        imageIOUtil.writeImage(image, suffix, out);
                    }
                }
            }
            else if ("jp2".equals(suffix))
            {
                String colorSpaceName = pdImage.getColorSpace().getName();
                if (directJPEG
                        || (PDDeviceGray.INSTANCE.getName().equals(colorSpaceName)
                        || PDDeviceRGB.INSTANCE.getName().equals(colorSpaceName)))
                {
                    // RGB or Gray colorspace: get and write the unmodified JPEG2000 stream
                    InputStream data = pdImage.createInputStream(
                            Arrays.asList(COSName.JPX_DECODE.getName()));
                    IOUtils.copy(data, out);
                    IOUtils.closeQuietly(data);
                }
                else
                {
                    // for CMYK and other "unusual" colorspaces, the image will be converted
                    BufferedImage image = pdImage.getImage();
                    if (image != null)
                    {
                        imageIOUtil.writeImage(image, "jpeg2000", out);
                    }
                }
            }
            else if ("tiff".equals(suffix) && pdImage.getColorSpace().equals(PDDeviceGray.INSTANCE))
            {
                BufferedImage image = pdImage.getImage();
                if (image == null)
                {
                    return;
                }
                // CCITT compressed images can have a different colorspace, but this one is B/W
                // This is a bitonal image, so copy to TYPE_BYTE_BINARY
                // so that a G4 compressed TIFF image is created by ImageIOUtil.writeImage()
                int w = image.getWidth();
                int h = image.getHeight();
                BufferedImage bitonalImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
                // copy image the old fashioned way - ColorConvertOp is slower!
                for (int y = 0; y < h; y++)
                {
                    for (int x = 0; x < w; x++)
                    {
                        bitonalImage.setRGB(x, y, image.getRGB(x, y));
                    }
                }
                imageIOUtil.writeImage(bitonalImage, suffix, out);
            }
            else
            {
                BufferedImage image = pdImage.getImage();
                if (image != null)
                {
                    imageIOUtil.writeImage(image, suffix, out);
                }
            }
            out.flush();
        }
    }

    private boolean hasMasks(PDImage pdImage) throws IOException
    {
        if (pdImage instanceof PDImageXObject)
        {
            PDImageXObject ximg = (PDImageXObject) pdImage;
            return ximg.getMask() != null || ximg.getSoftMask() != null;
        }
        return false;
    }
}