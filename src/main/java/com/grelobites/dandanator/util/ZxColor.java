package com.grelobites.dandanator.util;

public enum ZxColor {
	BLACK(0xFF000000),
	BLUE(0xFF0000CD),
	RED(0xFFCD0000),
	MAGENTA(0xFFCD00CD),
	GREEN(0xFF00CD00),
	CYAN(0xFF00CDCD),
	YELLOW(0xFFCDCD00),
	WHITE(0xFFCDCDCD),
	BRIGHTBLACK(0xFF000000),
	BRIGHTBLUE(0xFF0000FF),
	BRIGHTRED(0xFFFF0000),
	BRIGHTMAGENTA(0xFFFF00FF),
	BRIGHTGREEN(0xFF00FF00),
	BRIGHTCYAN(0xFF00FFFF),
	BRIGHTYELLOW(0xFFFFFF00),
	BRIGHTWHITE(0xFFFFFF);
	
	private static ZxColor[] COLOR_ARRAY = {
			BLACK, BLUE, RED, MAGENTA, GREEN,
			CYAN, YELLOW, WHITE, BRIGHTBLACK, 
			BRIGHTBLUE, BRIGHTRED, BRIGHTMAGENTA, 
			BRIGHTGREEN, BRIGHTCYAN, BRIGHTYELLOW, BRIGHTWHITE
	};
	
	private final int argb;
	
	ZxColor(int argb) {
		this.argb = argb;
	}
	
	public int argb() {
		return argb;
	}
	
	public static int byIndex(int index) {
		return COLOR_ARRAY[index].argb();
	}

}
