package com.kos.artower.java.ar.heroes;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.kos.artower.java.ar.ColoredAnchor;

import java.util.Random;

public class Game {

	public static final int MAX_ENEMIES = 50;
	public static final int MAX_CORES = 30;
	public static final int MAX_TARGETS = 30;

	public static final int FIND_SURFACE = 1;
	public static final int TOWER_ANCHOR = 2;
	public static final int GAME = 3;

	public int readyState = FIND_SURFACE;


	public long globalTime = 0L;
	public long frameDuration = 0L;
	public long gameTime = 0L;
	public long score = 0L;
	public boolean scoreIsUpdated = false; //Флаг что значение очков изменилось


	public ColoredAnchor[] anchors = new ColoredAnchor[MAX_TARGETS];

	@NonNull
	public Tower tower = new Tower();

	@NonNull
	public CoreInfo core = new CoreInfo();

	@NonNull
	public Enemy[] enemies = new Enemy[MAX_ENEMIES];

	private float waveEnemyTime = 20; //Время в секундах
	private int waveNumber = 1;


	public int shotCounter = 0;

	public Anchor towerAnchor;

	@NonNull
	public Random r = new Random(System.currentTimeMillis() * 23);

	public Game() {
		resetGame();

	}

	/**
	 * Сброс настроек игры, чтобы начать заново
	 */
	public void resetGame() {
		updateTime();
		gameTime = 0;
		updateTime();

		for (int i = 0; i < enemies.length; i++) {
			enemies[i] = new Enemy();
		}


		for (int i = 0; i < anchors.length; i++) {
			anchors[i] = new ColoredAnchor();
		}

		tower = new Tower();
		core = new CoreInfo();

		waveNumber = 0;
		waveEnemyTime = 20; //Время в секундах
		score = 0;

		shotCounter = 0;
		scoreIsUpdated = true;

	}

	public long updateTime() {
		Long oldTime = globalTime;
		globalTime = System.currentTimeMillis();
		frameDuration = globalTime - oldTime;
		gameTime += frameDuration;
		return frameDuration;
	}

	public void moveObjects() {
		newEnemies();
		moveEnemies();
		moveCores();
		intersectEnemies();


	}


	private void intersectEnemies() {
		if (towerAnchor != null) {
			Pose towerPose = towerAnchor.getPose();

			intersectShot(towerPose);
			for (Enemy enemy : enemies) {
				if (enemy.inGame()) {
					if (enemy.x < tower.radius) {
						changeHealth(-enemy.power);
						enemy.destroy();
					}
				}
			}
			intersectTowerAnchors(towerPose);
		}
	}

	private void intersectTowerAnchors(Pose towerPose) {
		for (ColoredAnchor anchor : anchors) {
			if (anchor.isShooting()) {
				float dx = anchor.coreX - towerPose.tx();
				float dz = anchor.coreZ - towerPose.tz();

				if (dx * dx + dz * dz < (tower.minRadius + core.radius) * (tower.minRadius + core.radius)) {


					if (anchor.coreY > towerPose.ty() && anchor.coreY < tower.height) {
						changeHealth(-anchor.power);
						addScore(1);
						anchor.destroy();
					}
				}
			}
		}
	}

	private void intersectShot(Pose towerPose) {
		if (towerAnchor != null) {
			for (ColoredAnchor anchor : anchors) {
				if (anchor.isShooting()) {
					for (Enemy enemy : enemies) {
						if (enemy.inGame()) {

							float tx = towerPose.tx() + (float) (Math.cos(enemy.angle / 180 * Math.PI) * enemy.x);
							float ty = towerPose.ty();
							float tz = towerPose.tz() - (float) (Math.sin(enemy.angle / 180 * Math.PI) * enemy.x);


							float dx = anchor.coreX - tx;
							float dy = Math.abs(anchor.coreY - ty);
							float dz = anchor.coreZ - tz;

							float powerRadius = (dy < core.radius) ? core.targetRadius : core.radius+enemy.radius;

							if (dx * dx + dz * dz < (powerRadius) * (powerRadius)) {
								if (dy < powerRadius) {
									anchor.readyDestroy();
									enemy.destroy();
									addScore(enemy.score);
								}
							}
						}//enemy in game
					}
				}//anchr shooting
				if (anchor.isReadyDestroy()){
					anchor.destroy();
				}
			}//for anchors
		}

	}

	private void addScore(int score) {
		this.score += score;
		scoreIsUpdated = true;
	}

	private void moveCores() {
		float f = frameDuration * 0.001f;

		for (ColoredAnchor anchor : anchors) {
			if (anchor.isShooting()) {
				float dx = anchor.coreX - anchor.anchor.tx();
				float dy = anchor.coreY - anchor.anchor.ty();
				float dz = anchor.coreZ - anchor.anchor.tz();


				float distance = f * anchor.speed;

				float sum = dx * dx + dy * dy + dz * dz;
				float d2 = distance * distance;
				float delta = (float) Math.sqrt(sum);

				if (sum <= d2) {
					anchor.coreX = anchor.anchor.tx();
					anchor.coreY = anchor.anchor.ty();
					anchor.coreZ = anchor.anchor.tz();
					anchor.readyDestroy();
				} else {
					anchor.coreX = anchor.coreX - dx*distance/delta;
					anchor.coreY = anchor.coreY - dy*distance/delta;
					anchor.coreZ = anchor.coreZ - dz*distance/delta;
				}
			}

		}

	}

	private void moveEnemies() {
		float f = frameDuration * 0.001f;

		for (Enemy enemy : enemies) {
			if (enemy.state == Enemy.State.Wait) {
				if (enemy.waitTime > 0) {
					enemy.waitTime -= f;
				} else {
					enemy.state = Enemy.State.Normal;
				}
			} else {
				enemy.animationPos = (enemy.animationPos + f) % enemy.animationLength;
				enemy.x -= enemy.vx * f;
				enemy.z -= enemy.vz * f;
				enemy.y = (float) (1f + (Math.cos((enemy.animationPos / enemy.animationLength) * 2 * Math.PI)) * 0.2f) * 0.1f;
			}

		}
	}


	private void newEnemies() {
		int c = enemiesCount();
		if (c == 0) {
			waveNumber += 1;
			waveEnemyTime += 0.100f;
			final int enemyInWave = Math.min(3 + waveNumber * 2, MAX_ENEMIES);

			float waitTime = waveEnemyTime / enemyInWave;

			for (int i = 0; i < enemyInWave; i++) {
				addEnemy(0, waitTime * i + r.nextFloat() * waitTime);
			}
		}
	}

	private boolean addEnemy(int meshIndex, float waitTime) {

		for (Enemy enemy : enemies) {
			if (enemy.isFree()) {
				enemy.reset(meshIndex, 0.5f + r.nextFloat() * 0.5f, r.nextFloat() * 360f,
						0.05f + r.nextFloat() * 0.005f,
						waitTime);
				return true;
			}
		}
		return false;
	}

	private int enemiesCount() {
		int c = 0;
		for (Enemy enemy : enemies) {
			if (!enemy.isFree())
				c++;
		}
		return c;
	}

	public void changeHealth(float changeValue) {
		tower.health += changeValue;

		if (tower.health > 100f)
			tower.health = 100f;

		if (tower.health <= 0) {
			gameOver();
			return;
		}

		tower.radius = (tower.maxRadius - tower.minRadius) * tower.health * 0.01f + tower.minRadius;
	}

	private void gameOver() {
		//Todo: Kos 14.11.2020  need game over state
		resetGame();
	}

	public void shot(Pose position) {

		for (ColoredAnchor anchor : anchors) {
			if (anchor.inGame() && !anchor.isShooting()) {
				anchor.coreX = position.tx();
				anchor.coreY = position.ty();
				anchor.coreZ = position.tz();

				anchor.speed = core.speed;

				anchor.setShooting(true);
			}
		}
	}

	public void addAnchor(Pose pose) {

		for (ColoredAnchor anchor : anchors) {
			if (!anchor.inGame()) {
				anchor.setup(pose);
				return;
			}
		}

	}
}
