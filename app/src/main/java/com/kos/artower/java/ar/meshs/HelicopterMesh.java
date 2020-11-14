package com.kos.artower.java.ar.meshs;

import com.kos.artower.java.common.samplerender.Mesh;
import com.kos.artower.java.common.samplerender.SampleRender;

import java.io.IOException;

public class HelicopterMesh {
	public Mesh helicopterMesh;
	public Mesh helicopterRotorMesh;
	public Mesh helicopterRotorTailMesh;

	public float[] color = {66.0f / 255.0f, 240.0f / 255.0f, 234.0f / 255.0f};;
	public float[] rotorColor = {22.0f / 255.0f, 50.0f / 255.0f, 22.0f / 255.0f};;
	public float rotorAngle = 0f;
	public float rotorTailAngle = 0f;
	public float[] tailRotorTranslate = {0, 0.05f,-0.38f};

	public HelicopterMesh(SampleRender render, String helicopterFile, String helicopterRotorFile, String helicopterTailRotorFile) throws IOException {
		helicopterMesh = Mesh.createFromAsset(render, helicopterFile);
		helicopterRotorMesh = Mesh.createFromAsset(render, helicopterRotorFile);
		helicopterRotorTailMesh = Mesh.createFromAsset(render, helicopterTailRotorFile);
	}

	public void moveRotors(long frameDuration) {
		rotorAngle=(rotorAngle+frameDuration*720*0.001f)%360.f;
		rotorTailAngle=(rotorTailAngle+frameDuration*330*0.001f)%360.f;
	}
}
