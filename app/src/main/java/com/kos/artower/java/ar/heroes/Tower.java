package com.kos.artower.java.ar.heroes;

public class Tower {
	public float radius;
	public float maxRadius;
	public float minRadius;
	public float health; //Здоровье башни
	public float height;

	public Tower() {
		maxRadius = 0.2f;
		minRadius = 0.08f;
		radius = maxRadius;
		height = 0.4f;
		health = 100f;

	}


}
