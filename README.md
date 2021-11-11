# Apache PDFBox Connector
This is a custom community connector for Mule 4 built with the Java SDK. It provides the same operations than PDFBox CLI:
  - Overlay PDFs
  - PDFs Merger
  - PDF Split
  - Extract Text
  - Extract Images
  - PDF to Image
  - Text to PDF

### Why?
There are many libraries that allow you to manipulate PDF files, but all are limited when it is necessary to scale in terms of license. All of these providers require acquiring an enterprise license. With Apache PDFBox corporate code can be shipped and delivered without the need for a special license.

Reference: https://www.apache.org/licenses/

### How?
This connector invokes the methods exposed by the Apache PDFBox SDK and abstracts the complexity of configuration and use exposing Mule operations.

### Usage
#### Development & publishing Commands 
The following Maven commands are required during development phase

| Task | Command |
| ------ | ------ |
| Compile during the development cycle | mvn clean package |
| Package connector and local installation (to .m2 folder)| mvn clean install |
| Publish to Exchange | mvn deploy (require maven distribution config)|

#### Development in Studio 
- Add this dependency to your application pom.xml
```
<groupId>com.mulesoft.connectors</groupId>
<artifactId>apache-pdfbox-connector</artifactId>
<version>1.0.0-SNAPSHOT</version>
<classifier>mule-plugin</classifier>
```
- Drag and drop the operation from the Mule Palette
- Configure the operation

#### Operations

##### Overlay PDF
This operation will overlay one document with the content of another document placing the watermark in one of the two available positions: FOREGROUND and BACKGROUND.
The attributes/fields this operation supports are:

| Field | Tab | Description |
| ------ | ------ | ------ |
| Input File | General | Path to the file to read |
| Output File | General | Path to the file to write |
| Overlay File | Overlay | Path to the watermark file |
| Position | Overlay | Enum. Position of the watermark. {FOREGORUND|BACKGROUND}. Default BACKGROUND |
| Pages Behavior | Overlay | Enum. Indicates if the watermark should be applied to {ODD|EVEN|FIRST|LAST|ALL} page/s. Default ALL |
| Page | Overlay |  OPTIONAL. Indicate the page number to apply the watermark |


##### PDF Merger
This operation will take a list of pdf documents and merge them, saving the result in a new document, given an output file name.

The attributes/fields this operation supports are:

| Field | Tab | Description |
| ------ | ------ | ------ |
| Input Files | General | Array of String. Path to the files to merge |
| Output File | General | Path to the file to write |


##### PDF Split
This operation will take an existing PDF document and split it into a number of new documents, given a prefix to use for the output files.

The attributes/fields this operation supports are:

| Field | Tab | Description |
| ------ | ------ | ------ |
| Input File | General | Path to the file to read |
| Output Prefix | General | Prefix of the output file. Can be used to specify the path as well |
| Start Page | General | OPTIONAL. From page |
| End Page | General | OPTIONAL. To page |
| Split | General | OPTIONAL. Number of pages of every splitted part of the pdf|
| Password | General |  OPTIONAL. Indicate the page number to apply the watermark |

##### Extract Text
This operation will extract all text from the given PDF document and save the result in an output file.

The attributes/fields this operation supports are:

| Field | Tab | Description |
| ------ | ------ | ------ |
| Input File | General | Path to the file to read |
| Output File | General | Path to the file to write |
| Start Page | General | OPTIONAL. From page |
| End Page | General | OPTIONAL. To page |
| Password | General |  OPTIONAL. Indicate the password to open the input file (if any) |
| To Console | General | OPTIONAL. Send text to console in addition to a file |
| To HTML | General | OPTIONAL. Output in HTML format instead of raw text |
| Sort | General | OPTIONAL. Sort the text before writing |
| Separate Beads | General | OPTIONAL. Disables the separation by beads |
| Always Next | General | OPTIONAL. Process next page (if applicable) despite IOException. ignored when To HTML is set |
| Rotation Magic | General | OPTIONAL. Analyze each page for rotated text, rotate to 0° and extract separately. This is slower, and ignored when To HTML is set|

##### Extract Images
This operation will extract all images from the given PDF document, storing these on files named based on a given prefix and a counter order i.e. prefix "output" used in a document containing 3 images will generate 3 files, called output-1.jpeg, output-2.jpeg and output-3.jpeg (assuming the output format is jpeg).

The attributes/fields this operation supports are:

| Field | Tab | Description |
| ------ | ------ | ------ |
| Input File | General | Path to the file to read |
| Prefix | General | Prefix of the output file. Can be used to specify the path as well  |
| Password | General |  OPTIONAL. Indicate the password to open the input file (if any) |
| Use Direct JPEG | Image Format | OPTIONAL. Forces the extraction of JPEG images regardless of color |
| No Color Convertion | Image Format | OPTIONAL. Forces the color convertion to RGB |

##### PDF to Image
This operation will create one image for every page in the PDF document specified as input.

##### Text to PDF
This operation will create a PDF document from a text file given as input.

The attributes/fields this operation supports are:

| Field | Tab | Description |
| ------ | ------ | ------ |
| Input File | General | Path to the file to read |
| Output File | General | Path to the file to write |
| ttf Location | PDF Format |  OPTIONAL. Indicate the customn ttf location file to use |
| Font Size | PDF Format | OPTIONAL. Forces the extraction of JPEG images regardless of color |
| Page Size | PDF Format | Enum. The size of the page. Legal, Letter, A0 to A6.  |
| Landscape | PDF Format | Boolean. Use landscape format |

### Used Dependencies
This connector relies in the Apache PDFBox module to perform all the operations. The version used by this connector is 3.0.0-SNAPSHOT (sep '20).


### Enable Wire logging
Add the following logger to the log4j2.xml file:
```
<AsyncLogger name="org.mule.extension.apache.pdfbox" level="DEBUG"/>
```

### Known Issues
- Fonts permission issue: while working with operations that require custom fonts management, you could see an error similar to the next:
  ```
  ERROR 2020-09-14 10:48:08,867 [[MuleRuntime].cpuLight.22: [pdfbox-conn-poc].TextToPDF.CPU_LITE @5f792a99] [event: ec248000-f690-11ea-8db1-f01898419c56] org.apache.pdfbox.pdmodel.font.FileSystemFontProvider: Could not load font file: /Library/Fonts/SalesforceSans-Regular.ttf
  java.io.FileNotFoundException: /Library/Fonts/SalesforceSans-Regular.ttf (Permission denied)
  ```
  This is caused because the PDFBox uses a model created in memory to parse and manipulate documents and this model requires access to few system's resources, like fonts. In order to solve this issue (that can be ignored but, I know, is annoying), set the proper permission level (chmod) to the font file indicated on the java.io.FileNotFoundException output.  

### Contribution

Want to contribute? Great! You have two options:
1. (the preffered one, if you're a repo contributor) Create a feature branch using the convention  ```feature/your-feature-name```. Open a pull request against develop branch.
2. Fork the repo, make your updates and open a pull request.

### Release Version:
 - ***v1.0.0-SNAPSHOT***
   - Description: initial SNAPSHOT version 
   - To-do's:
     - Add support for base64 strings as input/output instead files (high)
     - Create custom exceptions to type generic errors like IOException (mid)
     - Create more enums to provide a better user interface for the connector (low)
     - Provide more tests (low)
	   - Provide more usage Examples
