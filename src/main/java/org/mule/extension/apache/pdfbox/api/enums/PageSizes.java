package org.mule.extension.apache.pdfbox.api.enums;

public enum PageSizes {
    LETTER("LETTER"),
    LEGAL("LEGAL"),
    A0("A0"),
    A1("A1"),
    A2("A2"),
    A3("A3"),
    A4("A4"),
    A5("A5"),
    A6("A6");

    private String size;

    PageSizes(String size) {
        this.size = size;
    }

    public String getSize() {
        return size;
    }
}

