package com.kos.artower.java.ar;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;

// Anchors created from taps used for object placing with a given color.
public class ColoredAnchor {

	private static final int STATE_DESTROY = 0;
	private static final int STATE_READY_DESTROY = 1;
	private static final int STATE_NORMAL = 2;

	public Pose anchor = Pose.IDENTITY;
	private boolean shooting = false;
	private int state = STATE_DESTROY;
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
		state = STATE_NORMAL;
		//Координата пули
		coreX = 0f;
		coreY = 0f;
		coreZ = 0f;

		speed = 1.f;
		power = 10f;
	}

	public boolean inGame(){
		return state > STATE_DESTROY;
	}
	public boolean isShooting(){
		return inGame() && shooting;
	}


	public boolean isReadyDestroy(){
		return 	this.state == STATE_READY_DESTROY;
	}

	public void destroy(){
		this.state = STATE_DESTROY;
		this.shooting = false;
	}

	public void readyDestroy(){
		this.state = STATE_READY_DESTROY;
	}

	public void setShooting(boolean value){
		this.shooting =  value;
	}


}
