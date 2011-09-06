package com.puzzleduck.targetLiveWallpaper;

public class FlareData {
	private float x;
	private float y;
	private float time;
	private float triggerTime;
	private float tilt;
	private int color1;
	private int color2;
	private int type;

	public FlareData() {
		x = 100;
		y = 100;
		time = 0;
		triggerTime = 50;
		tilt = 0;
		color1 = 0xFF00FF00;
		color2 = 0xFF0000FF;
		type = 0;
	}
	
	public FlareData(float inx, float iny, float intilt, int incolor1, int incolor2, int intype)
	{
		x = inx;
		y = iny;
		time = 0;
		triggerTime = 50;
		tilt = intilt;
		color1 = incolor1;
		color2 = incolor2;
		type = intype;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getTime() {
		return time;
	}

	public void setTime(float time) {
		this.time = time;
	}

	public float getTriggerTime() {
		return triggerTime;
	}

	public void setTriggerTime(float triggerTime) {
		this.triggerTime = triggerTime;
	}

	public float getTilt() {
		return tilt;
	}

	public void setTilt(float tilt) {
		this.tilt = tilt;
	}

	public int getColor1() {
		return color1;
	}

	public void setColor1(int color1) {
		this.color1 = color1;
	}

	public int getColor2() {
		return color2;
	}

	public void setColor2(int color2) {
		this.color2 = color2;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}