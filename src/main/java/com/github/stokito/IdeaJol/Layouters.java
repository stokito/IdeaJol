package com.github.stokito.IdeaJol;

import org.openjdk.jol.datamodel.X86_32_DataModel;
import org.openjdk.jol.datamodel.X86_64_COOPS_DataModel;
import org.openjdk.jol.datamodel.X86_64_DataModel;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.layouters.RawLayouter;

public class Layouters {
    public static final X86_32_DataModel MODEL_32 = new X86_32_DataModel();
    public static final X86_64_DataModel MODEL_64 = new X86_64_DataModel();
    public static final X86_64_COOPS_DataModel MODEL_64_COOPS = new X86_64_COOPS_DataModel();
    public static final X86_64_COOPS_DataModel MODEL_64_COOPS_16 = new X86_64_COOPS_DataModel(16);
    public static final Layouter[] LAYOUTERS = {
            new RawLayouter(MODEL_32),
            new RawLayouter(MODEL_64),
            new RawLayouter(MODEL_64_COOPS),
            new HotSpotLayouter(MODEL_32),
            new HotSpotLayouter(MODEL_64),
            new HotSpotLayouter(MODEL_64_COOPS),
            new HotSpotLayouter(MODEL_64_COOPS_16)
    };
}
