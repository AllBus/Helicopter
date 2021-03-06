package com.kos.artower.java.ar.heroes.moves;

import com.google.ar.core.Pose;
import com.kos.artower.java.ar.heroes.Enemy;

public class HealthEnemyMover implements IEnemyMover {
	@Override
	public void move(Enemy enemy, float duration, Pose towerPose) {
		enemy.animationPos = (enemy.animationPos + duration) % enemy.animationLength;

		float d = duration * enemy.speed;

		float dx = towerPose.tx()-enemy.x;
		float dz = towerPose.tz()-enemy.z;
		float distance = (float) Math.sqrt(dx*dx+dz*dz);

		enemy.x += dx*d/distance;
		enemy.z += dz*d/distance;
		enemy.y =  towerPose.ty();
	}
}
