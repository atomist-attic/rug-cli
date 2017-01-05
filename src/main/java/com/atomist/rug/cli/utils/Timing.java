package com.atomist.rug.cli.utils;

import java.math.BigDecimal;

public class Timing {

    private long start = System.currentTimeMillis();

    public float duration() {
        return round((System.currentTimeMillis() - start) / 1000F, 2).floatValue();
    }
    
    private BigDecimal round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);       
        return bd;
    }

}
