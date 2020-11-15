package com.kos.artower.java.ar;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;

// Anchors created from taps used for object placing with a given color.
public class ColoredAnchor {

	private static final int STATE_DESTROY = 0;
	private static final int STATE_READY_DESTROY = 1;
	private static final int STATE_START_DESTROY = 2;
	private static final int STATE_NORMAL = 3;

	public Pose anchor = Pose.IDENTITY;
	private boolean shooting = false;
	private int state = STATE_DESTROY;
	//Координата пули
	public float coreX = 0f;
	public float coreY = 0f;
	public float coreZ = 0f;

	public float speed = 1.f;
	public float power = 10f;

	public float powerAnimationTime = 0;
	public float maxPowerTime = 0.3f;

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
		powerAnimationTime =  0;
		maxPowerTime = 0.3f;
	}

	public boolean inGame(){
		return state > STATE_DESTROY;
	}
	public boolean isShooting(){
		return inGame() && shooting && !isPower();
	}
	public boolean isPower(){ return state == STATE_START_DESTROY;
	}

	public boolean isReadyDestroy(){
		return this.state == STATE_READY_DESTROY;
	}

	public void destroy(){
		this.state = STATE_DESTROY;
		this.shooting = false;
	}

	public void startDestroy(){
		this.state = STATE_START_DESTROY;
		shooting = false;
		powerAnimationTime =  0;
	}

	public void readyDestroy(){
		this.state = STATE_READY_DESTROY;
	}

	public void setShooting(boolean value){
		this.shooting =  value;
	}




}
