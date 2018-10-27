package com.itpa.mvideo.misc;

public class BusInfo {
    public String id;
    public String name;
    public String wifiName;

    // 0: FIU with English only, 1: Sweetwater with English/Spanish
    public int uiStyle;

    BusInfo(String id, String name, String wifiName, int uiStyle) {
        this.id = id;
        this.name = name;
        this.wifiName = wifiName;
        this.uiStyle = uiStyle;
    }
}
