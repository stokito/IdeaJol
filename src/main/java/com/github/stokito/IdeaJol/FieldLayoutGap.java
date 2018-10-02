package com.github.stokito.IdeaJol;

import org.openjdk.jol.info.FieldData;
import org.openjdk.jol.info.FieldLayout;

/** Stub class which represents gaps between fields: "(object header)" "(alignment/padding gap)", "(loss due to the next object alignment)" */
public class FieldLayoutGap extends FieldLayout {
    private String description;
    public FieldLayoutGap(FieldData fieldData, long offset, long size) {
        super(fieldData, offset, size);
    }

    public FieldLayoutGap(long offset, long size, String description) {
        super(null, offset, size);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
