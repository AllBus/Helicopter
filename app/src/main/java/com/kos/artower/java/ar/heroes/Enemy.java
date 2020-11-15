package com.kos.artower.java.ar.heroes;

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
	public float radius = 0.03f;


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

	public void reset(int meshIndex, float radius, float angle, float speed, float waitTime) {
		this.meshIndex = 0;
		this.x= radius; // (float) Math.sin(angle)*radius;
		this.y = 0;
		this.z = 0;//(float) Math.cos(angle)*radius;
		this.angle = angle;
		this.speed = 0;
		this.vy = 0;
		this.vz = 0;
		this.vx = speed;
		this.animationPos = 0;
		this.animationLength = 0.5f;
		this.state = State.Wait;
		this.waitTime = waitTime;
		this.power = 4f;
		this.score = 100;
	}

	public void destroy() {
		this.state = State.Free;
	}

	public enum State{
		Normal,
		Destroyed,
		Free,
		Wait
	}
}

