package com.kos.artower.java.ar.heroes;

import com.google.ar.core.Pose;

public class Enemy {

	public int meshIndex = 0;
	public float x;
	public float y;
	public float z;
	public float angle;
	public float speed;
	public float animationPos = 0.0f;
	public float vx = 0f;
	public float vy = 0f;
	public float vz = 0f;
	public float animationLength = 1.5f;
	public float waitTime = 0; //Время в секундах
	public float power = 4f;
	public int score = 100;
	public float radius = 0.025f;


	public Enemy.State state = State.Free;

	/**
	 * @return Герой свободен. Не существует
	 */
	public boolean isFree() {
		return state == State.Free;
	}

	/**
	 *
	 * @return Герой в игре и движется.
	 */
	public boolean inGame() { return state == State.Normal; }

	public void reset(int meshIndex, float power, Pose center, float radius, float angle, float speed, float waitTime) {
		this.meshIndex = meshIndex;
		this.x = center.tx() +(float) Math.sin(angle)*radius;
		this.y = center.ty();
		this.z = center.tz() +(float) -Math.cos(angle)*radius;
		this.angle = angle;
		this.speed = speed;
		this.vy = 0;
		this.vz = 0;
		this.vx = 0;
		this.animationPos = 0;
		this.animationLength = 0.5f;
		this.state = State.Wait;
		this.waitTime = waitTime;
		this.power = power;
		this.score = 100;
		this.radius = 0.025f;
	}

	public void destroy() {
		this.state = State.Free;
	}

	public void startWait(float duration) {
		if (waitTime > 0) {
			waitTime -= duration;
		} else {
			state = Enemy.State.Normal;
		}
	}

	public enum State{
		Normal,
		Destroyed,
		Free,
		Wait
	}
}

