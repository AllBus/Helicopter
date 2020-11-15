package com.kos.artower.java.ar;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;

// Anchors created from taps used for object placing with a given color.
public class ColoredAnchor {

	public final Pose anchor;
	public boolean shooting = false;
	public boolean destroy = false;
	//Координата пули
	public float coreX = 0f;
	public float coreY = 0f;
	public float coreZ = 0f;

	public float speed = 1.f;
	public float power = 10f;

	public ColoredAnchor(Pose pose) {
		this.anchor = pose;

	}
}
