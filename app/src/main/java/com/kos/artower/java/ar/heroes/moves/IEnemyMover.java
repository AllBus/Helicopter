package com.kos.artower.java.ar.heroes.moves;

import com.google.ar.core.Pose;
import com.kos.artower.java.ar.heroes.Enemy;

public interface IEnemyMover {

	void move(Enemy enemy, float duration, Pose towerPose);
}
