package org.mule.extension.apache.pdfbox.internal.utils.imageio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public final class MetaUtil
{
    private static final Logger LOG = LoggerFactory.getLogger(MetaUtil.class);

    static final String SUN_TIFF_FORMAT = "com_sun_media_imageio_plugins_tiff_image_1.0";
    static final String JPEG_NATIVE_FORMAT = "javax_imageio_jpeg_image_1.0";
    static final String STANDARD_METADATA_FORMAT = "javax_imageio_1.0";

    private MetaUtil()
    {
    }

    static void debugLogMetadata(IIOMetadata metadata, String format)
    {

        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
        try
        {
            StringWriter xmlStringWriter = new StringWriter();
            StreamResult streamResult = new StreamResult(xmlStringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource domSource = new DOMSource(root);
            transformer.transform(domSource, streamResult);
            LOG.debug("\n" + xmlStringWriter);
        }
        catch (IllegalArgumentException | TransformerException ex)
        {
            LOG.error(ex.getMessage());
        }
    }

}
