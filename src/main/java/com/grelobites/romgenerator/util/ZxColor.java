package com.grelobites.romgenerator.util;

public enum ZxColor {
	BLACK(0, 0xFF000000),
	BLUE(1, 0xFF0000CD),
	RED(2, 0xFFCD0000),
	MAGENTA(3, 0xFFCD00CD),
	GREEN(4, 0xFF00CD00),
	CYAN(5, 0xFF00CDCD),
	YELLOW(6, 0xFFCDCD00),
	WHITE(7, 0xFFCDCDCD),
	BRIGHTBLACK(8, 0xFF000000),
	BRIGHTBLUE(9, 0xFF0000FF),
	BRIGHTRED(10, 0xFFFF0000),
	BRIGHTMAGENTA(11, 0xFFFF00FF),
	BRIGHTGREEN(12, 0xFF00FF00),
	BRIGHTCYAN(13, 0xFF00FFFF),
	BRIGHTYELLOW(14, 0xFFFFFF00),
	BRIGHTWHITE(15, 0xFFFFFFFF);
	
	private static ZxColor[] COLOR_ARRAY = {
			BLACK, BLUE, RED, MAGENTA, GREEN,
			CYAN, YELLOW, WHITE, BRIGHTBLACK, 
			BRIGHTBLUE, BRIGHTRED, BRIGHTMAGENTA, 
			BRIGHTGREEN, BRIGHTCYAN, BRIGHTYELLOW, BRIGHTWHITE
	};

	private final int index;
	private final int argb;
	
	ZxColor(int index, int argb) {
	    this.index = index;
		this.argb = argb;
	}
	
	public int argb() {
		return argb;
	}

	public int index() {
	    return index;
    }

	public static int byIndex(int index) {
		return COLOR_ARRAY[index].argb();
	}

	public static ZxColor indexed(int index) {
	    return COLOR_ARRAY[index];
    }

}
