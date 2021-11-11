package org.mule.extension.apache.pdfbox.internal.utils.imageio;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.DeflaterOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class imageIOUtil
{

    private static final Log LOG = LogFactory.getLog(imageIOUtil.class);

    private imageIOUtil()
    {
    }

    public static boolean writeImage(BufferedImage image, String filename,
                                     int dpi) throws IOException
    {
        float compressionQuality = 1f;
        String formatName = filename.substring(filename.lastIndexOf('.') + 1);
        if ("png".equalsIgnoreCase(formatName))
        {
            // PDFBOX-4655: prevent huge PNG files on jdk11 / jdk12 / jjdk13
            compressionQuality = 0f;
        }
        return writeImage(image, filename, dpi, compressionQuality);
    }

    public static boolean writeImage(BufferedImage image, String filename,
                                     int dpi, float compressionQuality) throws IOException
    {
        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(filename)))
        {
            String formatName = filename.substring(filename.lastIndexOf('.') + 1);
            return writeImage(image, formatName, output, dpi, compressionQuality);
        }
    }

    public static boolean writeImage(BufferedImage image, String formatName, OutputStream output)
            throws IOException
    {
        return writeImage(image, formatName, output, 72);
    }

    public static boolean writeImage(BufferedImage image, String formatName, OutputStream output,
                                     int dpi) throws IOException
    {
        float compressionQuality = 1f;
        if ("png".equalsIgnoreCase(formatName))
        {
            // PDFBOX-4655: prevent huge PNG files on jdk11 / jdk12 / jjdk13
            compressionQuality = 0f;
        }
        return writeImage(image, formatName, output, dpi, compressionQuality);
    }

    public static boolean writeImage(BufferedImage image, String formatName, OutputStream output,
                                     int dpi, float compressionQuality) throws IOException
    {
        return writeImage(image, formatName, output, dpi, compressionQuality, "");
    }

    public static boolean writeImage(BufferedImage image, String formatName, OutputStream output,
                                     int dpi, float compressionQuality, String compressionType) throws IOException
    {
        ImageOutputStream imageOutput = null;
        ImageWriter writer = null;
        try
        {
            // find suitable image writer
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
            ImageWriteParam param = null;
            IIOMetadata metadata = null;
            // Loop until we get the best driver, i.e. one that supports
            // setting dpi in the standard metadata format; however we'd also
            // accept a driver that can't, if a better one can't be found
            while (writers.hasNext())
            {
                if (writer != null)
                {
                    writer.dispose();
                }
                writer = writers.next();
                if (writer != null)
                {
                    param = writer.getDefaultWriteParam();
                    metadata = writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), param);
                    if (metadata != null
                            && !metadata.isReadOnly()
                            && metadata.isStandardMetadataFormatSupported())
                    {
                        break;
                    }
                }
            }
            if (writer == null)
            {
                LOG.error("No ImageWriter found for '" + formatName + "' format");
                LOG.error("Supported formats: " + Arrays.toString(ImageIO.getWriterFormatNames()));
                return false;
            }

            // compression
            if (param != null && param.canWriteCompressed())
            {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                if (formatName.toLowerCase().startsWith("tif"))
                {
                    if ("".equals(compressionType))
                    {
                        // default logic
                        TIFFUtil.setCompressionType(param, image);
                    }
                    else
                    {
                        param.setCompressionType(compressionType);
                        if (compressionType != null)
                        {
                            param.setCompressionQuality(compressionQuality);
                        }
                    }
                }
                else
                {
                    param.setCompressionType(param.getCompressionTypes()[0]);
                    param.setCompressionQuality(compressionQuality);
                }
            }

            if (formatName.toLowerCase().startsWith("tif"))
            {
                // TIFF metadata
                TIFFUtil.updateMetadata(metadata, image, dpi);
            }
            else if ("jpeg".equalsIgnoreCase(formatName)
                    || "jpg".equalsIgnoreCase(formatName))
            {
                JPEGUtil.updateMetadata(metadata, dpi);
            }
            else
            {
                // write metadata is possible
                if (metadata != null
                        && !metadata.isReadOnly()
                        && metadata.isStandardMetadataFormatSupported())
                {
                    setDPI(metadata, dpi, formatName);
                }
            }

            if (metadata != null && formatName.equalsIgnoreCase("png") && hasICCProfile(image))
            {
                IIOMetadataNode iccp = new IIOMetadataNode("iCCP");
                ICC_Profile profile = ((ICC_ColorSpace) image.getColorModel().getColorSpace())
                        .getProfile();
                iccp.setUserObject(getAsDeflatedBytes(profile));
                iccp.setAttribute("profileName", "unknown");
                iccp.setAttribute("compressionMethod", "deflate");
                Node nativeTree = metadata.getAsTree(metadata.getNativeMetadataFormatName());
                nativeTree.appendChild(iccp);
                metadata.mergeTree(metadata.getNativeMetadataFormatName(), nativeTree);
            }

            imageOutput = ImageIO.createImageOutputStream(output);
            writer.setOutput(imageOutput);
            writer.write(null, new IIOImage(image, null, metadata), param);
        }
        finally
        {
            if (writer != null)
            {
                writer.dispose();
            }
            if (imageOutput != null)
            {
                imageOutput.close();
            }
        }
        return true;
    }

    private static boolean hasICCProfile(BufferedImage image)
    {
        ColorSpace colorSpace = image.getColorModel().getColorSpace();
        // We can only export ICC color spaces
        if (!(colorSpace instanceof ICC_ColorSpace))
        {
            return false;
        }

        return !colorSpace.isCS_sRGB() && colorSpace != ColorSpace.getInstance(ColorSpace.CS_GRAY);
    }

    private static byte[] getAsDeflatedBytes(ICC_Profile profile) throws IOException
    {
        byte[] data = profile.getData();

        ByteArrayOutputStream deflated = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(deflated))
        {
            deflater.write(data);
        }

        return deflated.toByteArray();
    }

    private static IIOMetadataNode getOrCreateChildNode(IIOMetadataNode parentNode, String name)
    {
        NodeList nodeList = parentNode.getElementsByTagName(name);
        if (nodeList.getLength() > 0)
        {
            return (IIOMetadataNode) nodeList.item(0);
        }
        IIOMetadataNode childNode = new IIOMetadataNode(name);
        parentNode.appendChild(childNode);
        return childNode;
    }

    private static void setDPI(IIOMetadata metadata, int dpi, String formatName)
            throws IIOInvalidTreeException
    {
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(MetaUtil.STANDARD_METADATA_FORMAT);

        IIOMetadataNode dimension = getOrCreateChildNode(root, "Dimension");

        float res = "PNG".equalsIgnoreCase(formatName)
                ? dpi / 25.4f
                : 25.4f / dpi;

        IIOMetadataNode child;

        child = getOrCreateChildNode(dimension, "HorizontalPixelSize");
        child.setAttribute("value", Double.toString(res));

        child = getOrCreateChildNode(dimension, "VerticalPixelSize");
        child.setAttribute("value", Double.toString(res));

        metadata.mergeTree(MetaUtil.STANDARD_METADATA_FORMAT, root);
    }
}
