package org.mule.extension.apache.pdfbox.internal;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;

import org.mule.extension.apache.pdfbox.api.enums.PageSizes;
import org.mule.extension.apache.pdfbox.api.enums.PagesBehavior;
import org.mule.extension.apache.pdfbox.api.exceptions.InvalidColorException;
import org.mule.extension.apache.pdfbox.api.exceptions.NoWriterFoundException;
import org.mule.extension.apache.pdfbox.internal.operations.*;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.display.*;
import org.mule.extension.apache.pdfbox.api.enums.OverlayPosition;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.io.IOException;

/**
 * This class is a container for operations, every public method in this class will be taken as an extension operation.
 */
public class PDFBoxOperations {

  @DisplayName("OverlayPDF")
  @MediaType(value = ANY, strict = false)
  public String overlayPDF(String inputFile,
                           String outputFile,
                           @Placement(tab="Overlay") String overlayFile,
                           @Placement(tab="Overlay") @Optional(defaultValue="BACKGROUND") @Expression(NOT_SUPPORTED) OverlayPosition position,
                           @Placement(tab="Overlay") @Optional(defaultValue="ALL") @Expression(NOT_SUPPORTED) PagesBehavior pagesBehavior,
                           @Placement(tab="Overlay") @Optional String page) throws IOException{
    OverlayPDF overlay = new OverlayPDF();
    String message = overlay.OverlayPDF(inputFile, outputFile, overlayFile, position, pagesBehavior, page);
    return message;
  }

  @DisplayName("PDFMerger")
  @MediaType(value = ANY, strict = false)
  public String pdfMerger(String[] inputFiles, String outputFile) throws IOException{
    PDFMerger merger = new PDFMerger();
    String message = merger.PDFMerger(inputFiles, outputFile);
    return message;
  }

  @DisplayName("PDFSplit")
  @MediaType(value = ANY, strict = false)
  public String pdfSplit(String inputFile,
                         String outputPrefix,
                         @Optional String startPage,
                         @Optional String endPage,
                         @Optional String split,
                         @Optional @Password String password) throws IOException{
    PDFSplit splitter = new PDFSplit();
    String message = splitter.PDFSplit(inputFile, outputPrefix, startPage, endPage, split, password);
    return message;
  }

  @DisplayName("ExtractText")
  @MediaType(value = ANY, strict = false)
  public String extractText(String inputFile,
                            String outputFile,
                            @Optional String startPage,
                            @Optional String endPage,
                            @Optional @Password String password,
                            @Optional boolean toConsole,
                            @Optional boolean toHTML,
                            @Optional boolean sort,
                            @Optional boolean separateBeads,
                            @Optional boolean alwaysNext,
                            @Optional boolean rotationMagic) throws IOException{
    ExtractText extracter = new ExtractText();
    String message = extracter.ExtractText(inputFile, outputFile, password, toConsole, toHTML, sort,
                                           separateBeads,  alwaysNext, rotationMagic, startPage, endPage);
    return message;
  }

  @DisplayName("PDFToImage")
  @MediaType(value = ANY, strict = false)
  public String pdfToImage(String inputFile,
                           String outputPrefix,
                           @Optional(defaultValue="1")int startPage,
                           @Optional int endPage,
                           @Optional @Password String password,
                           @Placement(tab="Image Format") @Optional(defaultValue="jpg") String imageFormat,
                           @Placement(tab="Image Format") @Optional(defaultValue="rgb") String color,
                           @Placement(tab="Image Format") @Optional(defaultValue="96") int dpi,
                           @Placement(tab="Image Format") @Optional(defaultValue="-1") float quality,
                           @Placement(tab="Image Format") @Optional float cropBoxLowerLeftX,
                           @Placement(tab="Image Format") @Optional float cropBoxLowerLeftY,
                           @Placement(tab="Image Format") @Optional float cropBoxUpperRightX,
                           @Placement(tab="Image Format") @Optional float cropBoxUpperRightY,
                           boolean subsampling) throws IOException, NoWriterFoundException, InvalidColorException {
    PDFToImage extractor = new PDFToImage();
    String message = extractor.PDFToImage(inputFile, outputPrefix, startPage, endPage, password,
                                          imageFormat, color, dpi, quality, cropBoxLowerLeftX,
                                          cropBoxLowerLeftY, cropBoxUpperRightX, cropBoxUpperRightY,
                                          subsampling);
    return message;
  }

  @DisplayName("ExtactImages")
  @MediaType(value = ANY, strict = false)
  public String extactImages(String inputFile,
                             @Optional @Password String password,
                             String prefix,
                             @Placement(tab="Image Format") @Optional(defaultValue="true") boolean useDirectJPEG,
                             @Placement(tab="Image Format") @Optional(defaultValue="true") boolean noColorConvert) throws IOException {
    ExtractImages extractor = new ExtractImages();
    String message = extractor.ExtractImages(inputFile, password, prefix, useDirectJPEG, noColorConvert);
    return message;
  }

  @DisplayName("TextToPDF")
  @MediaType(value = ANY, strict = false)
  public String textToPDF(String inputFile,
                          String outputFile,
                          @Placement(tab="PDF Format") @Optional String ttfLocation,
                          @Placement(tab="PDF Format") @Optional String fontSize,
                          @Placement(tab="PDF Format") PageSizes pageSize,
                          @Placement(tab="PDF Format") @Optional(defaultValue="false") boolean landscape) throws IOException {
    TextToPDF text2pdf = new TextToPDF();
    String message = text2pdf.TextToPDF(inputFile, outputFile, ttfLocation, fontSize, pageSize, landscape);
    return message;
  }
}
