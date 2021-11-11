package org.mule.extension.apache.pdfbox.api.enums;

public enum OverlayPosition {
    FOREGROUND("FOREGROUND"),
    BACKGROUND("BACKGROUND");

    private String position;

    OverlayPosition(String position) {
        this.position = position;
    }

    public String getPosition() {
        return position;
    }
}
