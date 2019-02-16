package com.kresshy.weatherstation.force;

import lombok.Data;


// {"unit":"gram","force":0,"count":300}
@Data
public class ForceData {

    private String unit;
    private int force;
    private int count;

    public ForceData() {
        this.unit = "cm";
        this.force = 0;
        this.count = 0;
    }

    public ForceData(int force, int count) {
        this.unit = "cm";
        this.force = force;
        this.count = count;
    }

    public ForceData(String unit, int force, int count) {
        this.unit = unit;
        this.force = force;
        this.count = count;
    }
}
