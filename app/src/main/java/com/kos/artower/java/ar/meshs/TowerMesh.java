package com.kos.artower.java.ar.meshs;

import com.kos.artower.java.common.samplerender.Mesh;
import com.kos.artower.java.common.samplerender.SampleRender;
import com.kos.artower.java.common.samplerender.Shader;

import java.io.IOException;

public class TowerMesh {
	public Mesh mesh;
	public Mesh wallMesh;
	public Shader shader;
	public Shader wallShader;

	public TowerMesh(SampleRender render, String towerFile, String wallFile,
					 Shader towerShader, Shader wallShader) throws IOException {
		mesh = Mesh.createFromAsset(render, towerFile);
		wallMesh = Mesh.createFromAsset(render, wallFile);

		this.shader = towerShader;
		this.wallShader = wallShader;
	}
}
