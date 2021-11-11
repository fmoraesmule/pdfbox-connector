package org.mule.extension.apache.pdfbox.api.enums;

public enum PagesBehavior {
    ODD("ODD"),
    EVEN("EVEN"),
    FIRST("FIRST"),
    LAST("LAST"),
    ALL("ALL");

    private String pagesBehavior;

    PagesBehavior(String pagesBehavior) {
        this.pagesBehavior = pagesBehavior;
    }

    public String getPagesBehavior() {
        return pagesBehavior;
    }
}
