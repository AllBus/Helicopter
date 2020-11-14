package com.kos.artower.java.ar.meshs;

import com.kos.artower.java.common.samplerender.Mesh;
import com.kos.artower.java.common.samplerender.SampleRender;
import com.kos.artower.java.common.samplerender.Shader;
import com.kos.artower.java.common.samplerender.Texture;

import java.io.IOException;
import java.util.HashMap;

public class HelicopterMesh {
	public Mesh helicopterMesh;
	public Mesh helicopterRotorMesh;
	public Mesh helicopterRotorTailMesh;
	public Shader shader;


	public float[] color = {66.0f / 255.0f, 240.0f / 255.0f, 234.0f / 255.0f};;
	public float[] rotorColor = {22.0f / 255.0f, 50.0f / 255.0f, 22.0f / 255.0f};;
	public float rotorAngle = 0f;
	public float rotorTailAngle = 0f;
	public float[] tailRotorTranslate = {0, 0.05f,-0.38f};

	public HelicopterMesh(SampleRender render, String helicopterFile, String helicopterRotorFile, String helicopterTailRotorFile, String textureFile) throws IOException {
		helicopterMesh = Mesh.createFromAsset(render, helicopterFile);
		helicopterRotorMesh = Mesh.createFromAsset(render, helicopterRotorFile);
		helicopterRotorTailMesh = Mesh.createFromAsset(render, helicopterTailRotorFile);

		Texture virtualObjectTexture =
				Texture.createFromAsset(render, textureFile, Texture.WrapMode.CLAMP_TO_EDGE);

		shader = createVirtualObjectShader(
				render, virtualObjectTexture, /*use_depth_for_occlusion=*/ false);
	}

	public void moveRotors(long frameDuration) {
		rotorAngle=(rotorAngle+frameDuration*720*0.001f)%360.f;
		rotorTailAngle=(rotorTailAngle+frameDuration*330*0.001f)%360.f;
	}


	// Virtual object
	private static final String AMBIENT_INTENSITY_VERTEX_SHADER_NAME =
			"shaders/ambient_intensity.vert";
	private static final String AMBIENT_INTENSITY_FRAGMENT_SHADER_NAME =
			"shaders/ambient_intensity.frag";
	private static Shader createVirtualObjectShader(
			SampleRender render, Texture virtualObjectTexture, boolean useDepthForOcclusion)
			throws IOException {
		return Shader.createFromAssets(
				render,
				AMBIENT_INTENSITY_VERTEX_SHADER_NAME,
				AMBIENT_INTENSITY_FRAGMENT_SHADER_NAME,
				new HashMap<String, String>() {
					{
						put("USE_DEPTH_FOR_OCCLUSION", useDepthForOcclusion ? "1" : "0");
					}
				})
				.setBlend(Shader.BlendFactor.SRC_ALPHA, Shader.BlendFactor.ONE_MINUS_SRC_ALPHA)
				.setTexture("u_AlbedoTexture", virtualObjectTexture)
				.set1("u_UpperDiffuseIntensity", 1.0f)
				.set1("u_LowerDiffuseIntensity", 0.5f)
				.set1("u_SpecularIntensity", 0.2f)
				.set1("u_SpecularPower", 8.0f);
	}
}
