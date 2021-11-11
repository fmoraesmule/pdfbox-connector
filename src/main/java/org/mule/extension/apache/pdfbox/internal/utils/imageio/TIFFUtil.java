package org.mule.extension.apache.pdfbox.internal.utils.imageio;

import org.mule.extension.apache.pdfbox.internal.operations.PDFToImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.BufferedImage;

import static org.mule.extension.apache.pdfbox.internal.utils.imageio.MetaUtil.debugLogMetadata;

final class TIFFUtil
{
    private static final Logger LOG = LoggerFactory.getLogger(TIFFUtil.class);

    private TIFFUtil()
    {
    }

    public static void setCompressionType(ImageWriteParam param, BufferedImage image)
    {
        // avoid error: first compression type is RLE, not optimal and incorrect for color images
        // TODO expose this choice to the user?
        if (image.getType() == BufferedImage.TYPE_BYTE_BINARY &&
                image.getColorModel().getPixelSize() == 1)
        {
            param.setCompressionType("CCITT T.6");
        }
        else
        {
            param.setCompressionType("LZW");
        }
    }

    static void updateMetadata(IIOMetadata metadata, BufferedImage image, int dpi)
            throws IIOInvalidTreeException
    {
        String metaDataFormat = metadata.getNativeMetadataFormatName();
        if (metaDataFormat == null)
        {
            LOG.debug("TIFF image writer doesn't support any data format");
            return;
        }

        debugLogMetadata(metadata, metaDataFormat);

        IIOMetadataNode root = new IIOMetadataNode(metaDataFormat);
        IIOMetadataNode ifd;
        if (root.getElementsByTagName("TIFFIFD").getLength() == 0)
        {
            ifd = new IIOMetadataNode("TIFFIFD");
            root.appendChild(ifd);
        }
        else
        {
            ifd = (IIOMetadataNode)root.getElementsByTagName("TIFFIFD").item(0);
        }

        ifd.appendChild(createRationalField(282, "XResolution", dpi, 1));
        ifd.appendChild(createRationalField(283, "YResolution", dpi, 1));
        ifd.appendChild(createShortField(296, "ResolutionUnit", 2)); // Inch

        ifd.appendChild(createLongField(278, "RowsPerStrip", image.getHeight()));
        ifd.appendChild(createAsciiField(305, "Software", "PDFBOX"));

        if (image.getType() == BufferedImage.TYPE_BYTE_BINARY &&
                image.getColorModel().getPixelSize() == 1)
        {
            ifd.appendChild(createShortField(262, "PhotometricInterpretation", 0));
        }

        metadata.mergeTree(metaDataFormat, root);

        debugLogMetadata(metadata, metaDataFormat);
    }

    private static IIOMetadataNode createShortField(int tiffTagNumber, String name, int val)
    {
        IIOMetadataNode field, arrayNode, valueNode;
        field = new IIOMetadataNode("TIFFField");
        field.setAttribute("number", Integer.toString(tiffTagNumber));
        field.setAttribute("name", name);
        arrayNode = new IIOMetadataNode("TIFFShorts");
        field.appendChild(arrayNode);
        valueNode = new IIOMetadataNode("TIFFShort");
        arrayNode.appendChild(valueNode);
        valueNode.setAttribute("value", Integer.toString(val));
        return field;
    }

    private static IIOMetadataNode createAsciiField(int number, String name, String val)
    {
        IIOMetadataNode field, arrayNode, valueNode;
        field = new IIOMetadataNode("TIFFField");
        field.setAttribute("number", Integer.toString(number));
        field.setAttribute("name", name);
        arrayNode = new IIOMetadataNode("TIFFAsciis");
        field.appendChild(arrayNode);
        valueNode = new IIOMetadataNode("TIFFAscii");
        arrayNode.appendChild(valueNode);
        valueNode.setAttribute("value", val);
        return field;
    }

    private static IIOMetadataNode createLongField(int number, String name, long val)
    {
        IIOMetadataNode field, arrayNode, valueNode;
        field = new IIOMetadataNode("TIFFField");
        field.setAttribute("number", Integer.toString(number));
        field.setAttribute("name", name);
        arrayNode = new IIOMetadataNode("TIFFLongs");
        field.appendChild(arrayNode);
        valueNode = new IIOMetadataNode("TIFFLong");
        arrayNode.appendChild(valueNode);
        valueNode.setAttribute("value", Long.toString(val));
        return field;
    }

    private static IIOMetadataNode createRationalField(int number, String name, int numerator,
                                                       int denominator)
    {
        IIOMetadataNode field, arrayNode, valueNode;
        field = new IIOMetadataNode("TIFFField");
        field.setAttribute("number", Integer.toString(number));
        field.setAttribute("name", name);
        arrayNode = new IIOMetadataNode("TIFFRationals");
        field.appendChild(arrayNode);
        valueNode = new IIOMetadataNode("TIFFRational");
        arrayNode.appendChild(valueNode);
        valueNode.setAttribute("value", numerator + "/" + denominator);
        return field;
    }

}