package com.github.stokito.IdeaJol;

import org.openjdk.jol.info.FieldData;
import org.openjdk.jol.info.FieldLayout;

public class FieldLayoutPadding extends FieldLayout {
    private String description;
    public FieldLayoutPadding(FieldData fieldData, long offset, long size) {
        super(fieldData, offset, size);
    }

    public FieldLayoutPadding(long offset, long size, String description) {
        super(null, offset, size);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
