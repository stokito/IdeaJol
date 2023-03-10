package com.github.stokito.IdeaJol;

import org.openjdk.jol.datamodel.Model32;
import org.openjdk.jol.datamodel.Model64;
import org.openjdk.jol.layouters.HotSpotLayouter;
import org.openjdk.jol.layouters.Layouter;
import org.openjdk.jol.layouters.RawLayouter;

public class Layouters {
    public static final Model32 MODEL_32 = new Model32();
    public static final Model64 MODEL_64 = new Model64(false, false, 8);
    public static final Model64 MODEL_64_COOPS = new Model64(true, false, 8);
    public static final Model64 MODEL_64_COOPS_16 = new Model64(true, false, 16);
    public static final Model64 MODEL_64_COOPS_CCPS = new Model64(true, true, 8);
    public static final Model64 MODEL_64_COOPS_CCPS_16 = new Model64(true, true, 16);
    /** compressed class pointers */
    public static final Model64 MODEL_64_CCPS = new Model64(false, true, 8);
    /** compressed class pointers with 16 bit offset */
    public static final Model64 MODEL_64_CCPS_16 = new Model64(false, true, 16);

    public static final Layouter[] LAYOUTERS = {
            new RawLayouter(MODEL_32),
            new RawLayouter(MODEL_64),
            new RawLayouter(MODEL_64_COOPS),
            new HotSpotLayouter(MODEL_32, 8),
            new HotSpotLayouter(MODEL_64, 8),
            new HotSpotLayouter(MODEL_64_COOPS, 8),
            new HotSpotLayouter(MODEL_64_COOPS_16, 8),
//            new HotSpotLayouter(MODEL_64_COOPS_CCPS, 8),
//            new HotSpotLayouter(MODEL_64_COOPS_CCPS_16, 8),
            // JDK 15 uses a new layout
            new HotSpotLayouter(MODEL_32, 15),
            new HotSpotLayouter(MODEL_64, 15),
            new HotSpotLayouter(MODEL_64_COOPS, 15),
            new HotSpotLayouter(MODEL_64_COOPS_16, 15),
            // CCPS
            new HotSpotLayouter(MODEL_64_CCPS, 15),
            new HotSpotLayouter(MODEL_64_CCPS_16, 15),
    };

    public static final String[] LAYOUTERS_NAMES = {
            "Raw 32-bit",
            "Raw 64-bit",
            "Raw 64-bit, COOPS",
            "HotSpot 32-bit",
            "HotSpot 64-bit",
            "HotSpot JDK >= 8 64-bit, COOPS, CCPS",
            "HotSpot JDK >= 8 64-bit, COOPS, CCPS, 16-byte align",
            "HotSpot JDK >= 15, 32-bit",
            "HotSpot JDK >= 15, 64-bit",
            "HotSpot JDK >= 15, 64-bit, COOPS, CCPS",
            "HotSpot JDK >= 15, 64-bit, COOPS, CCPS, 16-byte align",
//            "HotSpot JDK 15, 64-bit, CCPS",
//            "HotSpot JDK 15, 64-bit, CCPS, 16-byte align"
    };

    /**
     * Default 5 is Hotspot 64 bit COOPS CCPS
     */
    public static final int DEFAULT_LAYOUTER_INDEX = 5;
}
