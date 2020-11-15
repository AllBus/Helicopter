package com.kos.artower.java.ar;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;

// Anchors created from taps used for object placing with a given color.
public class ColoredAnchor {

	public Pose anchor = Pose.IDENTITY;
	public boolean shooting = false;
	private boolean destroy = true;
	//Координата пули
	public float coreX = 0f;
	public float coreY = 0f;
	public float coreZ = 0f;

	public float speed = 1.f;
	public float power = 10f;

	public ColoredAnchor() {
	}

	public void setup(Pose pose){
		anchor = pose;
		shooting = false;
		destroy = false;
		//Координата пули
		coreX = 0f;
		coreY = 0f;
		coreZ = 0f;

		speed = 1.f;
		power = 10f;
	}

	public boolean inGame(){
		return !destroy;
	}
	public boolean isShooting(){
		return inGame() && shooting;
	}

	public void destroy(){
		this.destroy = true;
	}
}
