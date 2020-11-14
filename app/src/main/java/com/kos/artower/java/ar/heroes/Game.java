package com.kos.artower.java.ar.heroes;

import androidx.annotation.NonNull;

import java.util.Random;

public class Game {

	public static final int MAX_ENEMIES = 50;
	public static final int MAX_CORES = 30;

	public static final int FIND_SURFACE = 1;
	public static final int TOWER_ANCHOR = 2;
	public static final int GAME = 3;

	public int readyState = FIND_SURFACE;

	public long globalTime = 0L;
	public long frameDuration = 0L;
	public long gameTime = 0L;


	@NonNull
	public Tower tower = new Tower();

	@NonNull
	public Enemy[] enemies = new Enemy[MAX_ENEMIES];

	@NonNull
	public Enemy[] cores = new Enemy[MAX_CORES];

	private float waveEnemyTime = 20; //Время в секундах
	private int waveNumber = 1;

	@NonNull
	public Random r = new Random(System.currentTimeMillis() * 23);

	public Game() {
		resetGame();

	}

	public void resetGame() {
		updateTime();
		gameTime = 0;
		updateTime();

		for (int i = 0; i < enemies.length; i++) {
			enemies[i] = new Enemy();
		}
		for (int i = 0; i < cores.length; i++) {
			cores[i] = new Enemy();
		}

		tower = new Tower();

		waveNumber = 0;
		waveEnemyTime = 20; //Время в секундах



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

		for (Enemy enemy : enemies) {
			if (enemy.inGame() && enemy.x < tower.radius) {
				changeHealth(-enemy.power);
				enemy.destroy();
			}
		}
	}

	private void moveCores() {
		float f = frameDuration * 0.001f;

		for (Enemy enemy : cores) {
			enemy.x -= enemy.vx * f;
			enemy.y -= enemy.vy * f;
			enemy.z -= enemy.vz * f;
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

			float waitTime = waveEnemyTime/enemyInWave;

			for (int i = 0; i < enemyInWave; i++) {
				addEnemy(0,waitTime*i+r.nextFloat()*waitTime);
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
}
