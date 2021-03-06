/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kos.artower.java.ar;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.media.Image;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Config.InstantPlacementMode;
import com.google.ar.core.Coordinates2d;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.InstantPlacementPoint;
import com.google.ar.core.InstantPlacementPoint.TrackingMethod;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.kos.artower.java.ar.meshs.TowerMesh;
import com.kos.artower.java.common.helpers.CameraPermissionHelper;
import com.kos.artower.java.common.helpers.DepthSettings;
import com.kos.artower.java.common.helpers.DisplayRotationHelper;
import com.kos.artower.java.common.helpers.FullScreenHelper;
import com.kos.artower.java.common.helpers.InstantPlacementSettings;
import com.kos.artower.java.common.helpers.SnackbarHelper;
import com.kos.artower.java.common.helpers.TapHelper;
import com.kos.artower.java.common.helpers.TrackingStateHelper;
import com.kos.artower.java.common.samplerender.Mesh;
import com.kos.artower.java.common.samplerender.SampleRender;
import com.kos.artower.java.common.samplerender.Shader;
import com.kos.artower.java.common.samplerender.Texture;
import com.kos.artower.java.common.samplerender.VertexBuffer;
import com.kos.artower.java.common.samplerender.arcore.BackgroundRenderer;
import com.kos.artower.java.common.samplerender.arcore.PlaneRenderer;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.kos.artower.java.ar.heroes.Enemy;
import com.kos.artower.java.ar.heroes.Game;
import com.kos.artower.java.ar.meshs.HelicopterMesh;
import com.kos.artowerr.R;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class ArActivity extends AppCompatActivity implements SampleRender.Renderer {

	private static final String TAG = ArActivity.class.getSimpleName();

	private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";




	TextView scoreLabel;
	View readyFrame;
	Button readyButton;
	Game game = new Game();




	// Rendering. The Renderers are created here, and initialized when the GL surface is created.
	private GLSurfaceView surfaceView;

	private boolean installRequested;

	private Session session;
	private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
	private DisplayRotationHelper displayRotationHelper;
	private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
	private TapHelper tapHelper;
	private SampleRender render;

	private Texture depthTexture;
	private boolean calculateUVTransform = true;
	private PlaneRenderer planeRenderer;
	private BackgroundRenderer backgroundRenderer;
	private boolean hasSetTextureNames = false;

	private final DepthSettings depthSettings = new DepthSettings();
	private boolean[] depthSettingsMenuDialogCheckboxes = new boolean[2];

	private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
	private boolean[] instantPlacementSettingsMenuDialogCheckboxes = new boolean[1];
	// Assumed distance from the device camera to the surface on which user will try to place objects.
	// This value affects the apparent scale of objects while the tracking method of the
	// Instant Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE.
	// Values in the [0.2, 2.0] meter range are a good choice for most AR experiences. Use lower
	// values for AR experiences where users are expected to place objects on surfaces close to the
	// camera. Use larger values for experiences where the user will likely be standing and trying to
	// place an object on the ground or floor in front of them.
	private static final float APPROXIMATE_DISTANCE_METERS = 1.0f;

	// Point Cloud
	private static final String POINT_CLOUD_VERTEX_SHADER_NAME = "shaders/point_cloud.vert";
	private static final String POINT_CLOUD_FRAGMENT_SHADER_NAME = "shaders/point_cloud.frag";
	private VertexBuffer pointCloudVertexBuffer;
	private Mesh pointCloudMesh;
	private Shader pointCloudShader;
	// Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
	// was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
	private long lastPointCloudTimestamp = 0;

	// Virtual object
	private static final String AMBIENT_INTENSITY_VERTEX_SHADER_NAME =
			"shaders/ambient_intensity.vert";
	private static final String AMBIENT_INTENSITY_FRAGMENT_SHADER_NAME =
			"shaders/ambient_intensity.frag";
	// Note: the last component must be zero to avoid applying the translational part of the matrix.
	private static final float[] LIGHT_DIRECTION = {0.250f, 0.866f, 0.433f, 0.0f};
	private static final float hitEps = 0.1f;
	private Mesh virtualObjectMesh;

	private Mesh enemyOneMesh;
	private Mesh enemyTwoMesh;
	private Mesh healthMesh;
	private Mesh canonMesh;
	private Mesh coreMesh;
	private Mesh targetMesh;
	private Mesh powerMesh;

	private TowerMesh tower;
	private HelicopterMesh helicopter;

	private Mesh[] enemyMeshs;

	private Shader targetShader;
	private Shader powerShader;
	private Shader virtualObjectShader;
	private Shader virtualObjectDepthShader;


	private static final float MESH_SCALE = 0.05f;

	// Temporary matrix allocated here to reduce number of allocations for each frame.
	private final float[] modelMatrix = new float[16];
	private final float[] tempMatrix = new float[16];
	private final float[] viewMatrix = new float[16];
	private final float[] projectionMatrix = new float[16];
	private final float[] modelViewMatrix = new float[16]; // view x model
	private final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model

	private final float[] viewLightDirection = new float[4]; // view x LIGHT_DIRECTION
	private final float[] towerColor = {66.0f / 255.0f, 255.0f / 255.0f, 244.0f / 255.0f};
	private final float[] enemyColor = {255.0f / 255.0f, 255.0f / 255.0f, 123.0f / 255.0f};
	private final float[] healthColor = {122.0f / 255.0f, 255.0f / 255.0f, 123.0f / 255.0f};

	private final float[] targetColor = {240.0f / 255.0f, 22.0f / 255.0f, 22.0f / 255.0f};

	private final float[][] enemiesColors = new float[5][];

	private float centerPositionX = 0.0f;
	private float centerPositionY = 0.0f;

	public void setReadyState(int newReadyState){
		if (newReadyState!=game.readyState){
			game.readyState = newReadyState;

			if (readyFrame!=null) {
				readyFrame.setVisibility(game.readyState==Game.FIND_SURFACE?View.VISIBLE:View.GONE);
			}

			if (game.readyState == Game.GAME){
				game.resetGame();
			}

		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		surfaceView = findViewById(R.id.surfaceview);
		displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);
		scoreLabel = findViewById(R.id.scoreLabel);


		// Set up touch listener.
		tapHelper = new TapHelper(/*context=*/ this);
		surfaceView.setOnTouchListener(tapHelper);
		surfaceView.post(() -> {
			centerPositionX = surfaceView.getWidth()*0.5f;
			centerPositionY = surfaceView.getHeight()*0.5f;
		});

		// Set up renderer.
		render = new SampleRender(surfaceView, this, getAssets());

		installRequested = false;
		calculateUVTransform = true;

		depthSettings.onCreate(this);
		instantPlacementSettings.onCreate(this);
		ImageButton settingsButton = findViewById(R.id.settings_button);
		settingsButton.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						PopupMenu popup = new PopupMenu(ArActivity.this, v);
						popup.setOnMenuItemClickListener(ArActivity.this::settingsMenuClick);
						popup.inflate(R.menu.settings_menu);
						popup.show();
					}
				});

		readyFrame = findViewById(R.id.readyFrame);
		readyButton = findViewById(R.id.readyButton);

		readyButton.setOnClickListener((v)->{
				setReadyState( Game.TOWER_ANCHOR);
		});

		updateScore();
	}

	private void updateScore() {
		game.scoreIsUpdated = false;
		if (game.readyState == Game.GAME){
			scoreLabel.setText(getString(R.string.score, game.score));
			scoreLabel.setVisibility(View.VISIBLE);
		}else{
			scoreLabel.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();

	}

	/**
	 * Menu button to launch feature specific settings.
	 */
	protected boolean settingsMenuClick(MenuItem item) {
		if (item.getItemId() == R.id.depth_settings) {
			launchDepthSettingsMenuDialog();
			return true;
		} else if (item.getItemId() == R.id.instant_placement_settings) {
			launchInstantPlacementSettingsMenuDialog();
			return true;
		}
		return false;
	}

	@Override
	protected void onDestroy() {
		if (session != null) {
			// Explicitly close ARCore Session to release native resources.
			// Review the API reference for important considerations before calling close() in apps with
			// more complicated lifecycle requirements:
			// https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
			session.close();
			session = null;
		}

		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (session == null) {
			Exception exception = null;
			String message = null;
			try {
				switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
					case INSTALL_REQUESTED:
						installRequested = true;
						return;
					case INSTALLED:
						break;
				}

				// ARCore requires camera permissions to operate. If we did not yet obtain runtime
				// permission on Android M and above, now is a good time to ask the user for it.
				if (!CameraPermissionHelper.hasCameraPermission(this)) {
					CameraPermissionHelper.requestCameraPermission(this);
					return;
				}

				// Create the session.
				session = new Session(/* context= */ this);
			} catch (UnavailableArcoreNotInstalledException
					| UnavailableUserDeclinedInstallationException e) {
				message = getString(R.string.arErrorInstallARCore);
				exception = e;
			} catch (UnavailableApkTooOldException e) {
				message = getString(R.string.arErrorUpdateARCore);
				exception = e;
			} catch (UnavailableSdkTooOldException e) {
				message = getString(R.string.arErrorUpdateApp);
				exception = e;
			} catch (UnavailableDeviceNotCompatibleException e) {
				message = getString(R.string.arErrorNoSupportAR);
				exception = e;
			} catch (Exception e) {
				message = getString(R.string.arErrorCreateSession);
				exception = e;
			}

			if (message != null) {
				messageSnackbarHelper.showError(this, message);
				Log.e(TAG, "Exception creating session", exception);
				return;
			}
		}

		// Note that order matters - see the note in onPause(), the reverse applies here.
		try {
			configureSession();
			session.resume();
		} catch (CameraNotAvailableException e) {
			messageSnackbarHelper.showError(this, getString(R.string.arErrorCameraUnavailable));
			session = null;
			return;
		}

		surfaceView.onResume();
		displayRotationHelper.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (session != null) {
			// Note that the order matters - GLSurfaceView is paused first so that it does not try
			// to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
			// still call session.update() and get a SessionPausedException.
			displayRotationHelper.onPause();
			surfaceView.onPause();
			session.pause();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
		super.onRequestPermissionsResult(requestCode, permissions, results);
		if (!CameraPermissionHelper.hasCameraPermission(this)) {
			Toast.makeText(this, getString(R.string.arErrorCameraPermission), Toast.LENGTH_LONG)
					.show();
			if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
				// Permission denied with checking "Do not ask again".
				CameraPermissionHelper.launchPermissionSettings(this);
			}
			finish();
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
	}

	@Override
	public void onSurfaceCreated(SampleRender render) {
		// Prepare the rendering objects. This involves reading shaders and 3D model files, so may throw
		// an IOException.
		try {
			depthTexture = new Texture(render, Texture.Target.TEXTURE_2D, Texture.WrapMode.CLAMP_TO_EDGE);
			planeRenderer = new PlaneRenderer(render);
			backgroundRenderer = new BackgroundRenderer(render, depthTexture);

			// Point cloud
			pointCloudShader =
					Shader.createFromAssets(
							render,
							POINT_CLOUD_VERTEX_SHADER_NAME,
							POINT_CLOUD_FRAGMENT_SHADER_NAME,
							/*defines=*/ null)
							.set4("u_Color", new float[]{31.0f / 255.0f, 200.0f / 255.0f, 32.0f / 255.0f, 1.0f})
							.set1("u_PointSize", 3.0f);
			// four entries per vertex: X, Y, Z, confidence
			pointCloudVertexBuffer =
					new VertexBuffer(render, /*numberOfEntriesPerVertex=*/ 4, /*entries=*/ null);
			final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};
			pointCloudMesh =
					new Mesh(
							render, Mesh.PrimitiveMode.POINTS, /*indexBuffer=*/ null, pointCloudVertexBuffers);


			// Virtual object to render
			Texture virtualObjectTexture =
					Texture.createFromAsset(render, "models/andy.png", Texture.WrapMode.CLAMP_TO_EDGE);
			virtualObjectMesh = Mesh.createFromAsset(render, "models/tower.obj" , MESH_SCALE);
			virtualObjectShader =
					createVirtualObjectShader(
							render, virtualObjectTexture, /*use_depth_for_occlusion=*/ false);
			virtualObjectDepthShader =
					createVirtualObjectShader(render, virtualObjectTexture, /*use_depth_for_occlusion=*/ true)
							.setTexture("u_DepthTexture", depthTexture);

			//Helicopter shader
			Texture helicopterTexture =
					Texture.createFromAsset(render, "models/helicopter.png", Texture.WrapMode.CLAMP_TO_EDGE);

			Shader helicopterShader = createVirtualObjectShader(
					render, helicopterTexture, /*use_depth_for_occlusion=*/ false);

			Texture targetTexture =
					Texture.createFromAsset(render, "models/target.png", Texture.WrapMode.CLAMP_TO_EDGE);

			targetShader = createTargetShader(
					render, targetTexture, /*use_depth_for_occlusion=*/ true);

			Texture powerTexture =
					Texture.createFromAsset(render, "models/power.png", Texture.WrapMode.CLAMP_TO_EDGE);

			powerShader = createPowerShader(
					render, powerTexture, /*use_depth_for_occlusion=*/ true);

			//Meshs
			enemyOneMesh = Mesh.createFromAsset(render, "models/enemy_one.obj",MESH_SCALE);
			enemyTwoMesh = Mesh.createFromAsset(render, "models/enemy_two.obj",MESH_SCALE);
			healthMesh = Mesh.createFromAsset(render, "models/health.obj", MESH_SCALE);
			canonMesh = Mesh.createFromAsset(render, "models/canon.obj", MESH_SCALE);
			coreMesh = Mesh.createFromAsset(render, "models/core.obj", MESH_SCALE);
			targetMesh = Mesh.createFromAsset(render, "models/target.obj", MESH_SCALE);
			powerMesh = Mesh.createFromAsset(render, "models/power.obj", 2f);

			tower = new TowerMesh(render,"models/tower.obj", "models/tower_wall.obj", virtualObjectShader, virtualObjectShader, MESH_SCALE);
			helicopter = new HelicopterMesh(render,
					"models/helicopter.obj",
					"models/helicopter_rotor.obj",
					"models/helicopter_tail.obj",
					helicopterShader,
					virtualObjectShader,
					MESH_SCALE*0.2f
					);



			enemyMeshs = new Mesh[5];
			enemyMeshs[0] = enemyOneMesh;
			enemyMeshs[1] = enemyTwoMesh;
			enemyMeshs[2] = healthMesh;
			enemyMeshs[3] = canonMesh;
			enemyMeshs[4] = coreMesh;


			enemiesColors[0] = enemyColor;
			enemiesColors[1] = enemyColor;
			enemiesColors[2] = healthColor;
			enemiesColors[3] = enemyColor;
			enemiesColors[4] = enemyColor;

		} catch (IOException e) {
			Log.e(TAG, "Failed to read an asset file", e);
		}
	}

	@Override
	public void onSurfaceChanged(SampleRender render, int width, int height) {
		displayRotationHelper.onSurfaceChanged(width, height);
	}

	@Override
	public void onDrawFrame(SampleRender render) {
		if (session == null) {
			return;
		}

		if (!hasSetTextureNames) {
			session.setCameraTextureNames(new int[]{backgroundRenderer.getTextureId()});
			hasSetTextureNames = true;
		}

		// Notify ARCore session that the view size changed so that the perspective matrix and
		// the video background can be properly adjusted.
		displayRotationHelper.updateSessionIfNeeded(session);


		game.updateTime();
		game.moveObjects();

		if (game.scoreIsUpdated){
			runOnUiThread(this::updateScore);
		}

		try {
			// Obtain the current frame from ARSession. When the configuration is set to
			// UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
			// camera framerate.
			Frame frame = session.update();
			Camera camera = frame.getCamera();


			if (frame.hasDisplayGeometryChanged() || calculateUVTransform) {
				// The UV Transform represents the transformation between screenspace in normalized units
				// and screenspace in units of pixels.  Having the size of each pixel is necessary in the
				// virtual object shader, to perform kernel-based blur effects.
				calculateUVTransform = false;
				float[] transform = getTextureTransformMatrix(frame);
				virtualObjectDepthShader.setMatrix3("u_DepthUvTransform", transform);
			}

			if (camera.getTrackingState() == TrackingState.TRACKING
					&& session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
				// The rendering abstraction leaks a bit here. Populate the depth texture with the current
				// frame data.
				try (Image depthImage = frame.acquireDepthImage()) {
					GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture.getTextureId());
					GLES30.glTexImage2D(
							GLES30.GL_TEXTURE_2D,
							0,
							GLES30.GL_RG8,
							depthImage.getWidth(),
							depthImage.getHeight(),
							0,
							GLES30.GL_RG,
							GLES30.GL_UNSIGNED_BYTE,
							depthImage.getPlanes()[0].getBuffer());
					float aspectRatio = (float) depthImage.getWidth() / (float) depthImage.getHeight();
					virtualObjectDepthShader.set1("u_DepthAspectRatio", aspectRatio);
				} catch (NotYetAvailableException e) {
					// This normally means that depth data is not available yet. This is normal so we will not
					// spam the logcat with this.
				}
			}

			// Handle one tap per frame.
			//handleTap(frame, camera);
			HitResult hitTower = null;
			if (game.readyState == Game.FIND_SURFACE) {
				hitTower = hitPosition(frame, camera, centerPositionX, centerPositionY);
				if (hitTower!=null) {
					if (!checkHorizontalPose( hitTower.getHitPose())){
						hitTower = null;
					}

				}

			}else
			if (game.readyState == Game.TOWER_ANCHOR)	{
				hitTower = hitPosition(frame, camera, centerPositionX, centerPositionY);
				if (hitTower!=null){

					if (!checkHorizontalPose( hitTower.getHitPose())){
						hitTower = null;
					}

					if (game.towerAnchor!=null){
						game.towerAnchor.detach();
						game.towerAnchor = null;
					}

					game.towerAnchor = hitTower.createAnchor();
					setReadyState(Game.GAME);
				}
			}else
			if (game.readyState == Game.GAME){
				handleTap(frame, camera);
			}

			// If frame is ready, render camera preview image to the GL surface.
			backgroundRenderer.draw(render, frame, depthSettings.depthColorVisualizationEnabled());

			// Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
			trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

			// If not tracking, don't draw 3D objects, show tracking failure reason instead.
			if (camera.getTrackingState() == TrackingState.PAUSED) {
				messageSnackbarHelper.showMessage(
						this, TrackingStateHelper.getTrackingFailureReasonString(camera));
				return;
			}

			// Get projection matrix.
			camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);

			// Get camera matrix and draw.
			camera.getViewMatrix(viewMatrix, 0);


			// Compute lighting from average intensity of the image.
			// The first three components are color scaling factors.
			// The last one is the average pixel intensity in gamma space.
			final float[] colorCorrectionRgba = new float[4];
			frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

//			// Visualize tracked points.
//			// Use try-with-resources to automatically release the point cloud.
//			try (PointCloud pointCloud = frame.acquirePointCloud()) {
//				if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
//					pointCloudVertexBuffer.set(pointCloud.getPoints());
//					lastPointCloudTimestamp = pointCloud.getTimestamp();
//				}
//				Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
//				pointCloudShader.setMatrix4("u_ModelViewProjection", modelViewProjectionMatrix);
//				render.draw(pointCloudMesh, pointCloudShader);
//			}

			// No tracking error at this point. If we detected any plane, then hide the
			// message UI, otherwise show searchingPlane message.
			if (hasTrackingPlane()) {
				messageSnackbarHelper.hide(this);
			} else {
				messageSnackbarHelper.showMessage(this, SEARCHING_PLANE_MESSAGE);
			}

			// Visualize planes.
			planeRenderer.drawPlanes(
					render,
					session.getAllTrackables(Plane.class),
					camera.getDisplayOrientedPose(),
					projectionMatrix);

			Shader shader =
					depthSettings.useDepthForOcclusion() ? virtualObjectDepthShader : virtualObjectShader;



			if (game.readyState == Game.FIND_SURFACE) {
				if (hitTower != null) {
					hitTower.getHitPose().toMatrix(modelMatrix, 0);

					drawMesh(tower.mesh, towerColor, colorCorrectionRgba, modelMatrix, tower.shader);
				}
			}else
			if (game.readyState == Game.GAME)
				{

				//Is In Game
				drawTower(colorCorrectionRgba);

				// Отображение целей и пулей вертолёта
				for (ColoredAnchor coloredAnchor : game.anchors) {
					if (coloredAnchor.inGame()) {



						if (coloredAnchor.isShooting()) {
							Pose.IDENTITY.toMatrix(modelMatrix, 0);
							Matrix.translateM(modelMatrix, 0, coloredAnchor.coreX, coloredAnchor.coreY, coloredAnchor.coreZ);
							drawMesh(coreMesh, targetColor, colorCorrectionRgba, modelMatrix, shader);
						}

						if (coloredAnchor.isPower()){
							float time = coloredAnchor.powerAnimationTime;
							if (time<coloredAnchor.maxPowerTime){
								float scale = time/coloredAnchor.maxPowerTime *game.core.targetRadius;
								Pose.IDENTITY.toMatrix(modelMatrix, 0);
								Matrix.translateM(modelMatrix, 0, coloredAnchor.coreX, coloredAnchor.coreY, coloredAnchor.coreZ);
								Matrix.scaleM(modelMatrix, 0, scale, scale, scale);
								drawTargetMesh(powerMesh, targetColor, colorCorrectionRgba, modelMatrix, powerShader, time/coloredAnchor.maxPowerTime);
							}

						}else{
							coloredAnchor.anchor.toMatrix(modelMatrix, 0);
							Matrix.translateM(modelMatrix, 0, 0, 0.02f, 0);
							Matrix.rotateM(modelMatrix, 0, game.gameTime * 0.001f * 180, 0, 1, 0);
							drawTargetMesh(targetMesh, targetColor, colorCorrectionRgba, modelMatrix, targetShader, (game.gameTime*0.001f)%1.0f);
						}
					}
				}

//
//				{
//					ColoredAnchor anchor = game.anchors[0];
//					anchor.anchor.toMatrix(modelMatrix, 0);
//					Matrix.translateM(modelMatrix, 0, 0, 0.02f, 0);
//					Matrix.rotateM(modelMatrix, 0, game.gameTime * 0.001f * 180, 0, 1, 0);
//
//
//					boolean intersect = false;
//
//					for (Enemy enemy : game.enemies) {
//						if (enemy.inGame()) {
//
//							float tx = enemy.x;
//							float ty = enemy.y;
//							float tz = enemy.z;
//
//
//							float dx = anchor.anchor.tx() - tx;
//							float dy = Math.abs(anchor.anchor.ty() - ty);
//							float dz = anchor.anchor.tz() - tz;
//
//							float powerRadius  = game.core.targetRadius+enemy.radius;
//
//							if (dx * dx + dz * dz < (powerRadius) * (powerRadius)) {
//								if (dy < powerRadius) {
//									intersect = true;
//								}
//							}
//						}//enemy in game
//					}
//
//					drawTargetMesh(targetMesh, intersect? enemyColor : targetColor, colorCorrectionRgba, modelMatrix, targetShader , 1f);
//
//
//				}

				for (Enemy enemy : game.enemies) {
					if (enemy.state == Enemy.State.Normal){

//						float tx = towerPose.tx() + (float) (Math.cos(enemy.angle / 180 * Math.PI) * enemy.x);
//						float ty = towerPose.ty() + enemy.y;
//						float tz = towerPose.tz() - (float) (Math.sin(enemy.angle / 180 * Math.PI) * enemy.x);

						Pose.IDENTITY.toMatrix(modelMatrix, 0);
						Matrix.translateM(modelMatrix,0, enemy.x, enemy.y, enemy.z);
						Matrix.rotateM(modelMatrix,0, enemy.angle,0,1,0);
						drawMesh(enemyMeshs[enemy.meshIndex],enemiesColors[enemy.meshIndex],colorCorrectionRgba, modelMatrix, shader);

					}
				}

				drawHelicopter(camera, modelMatrix, colorCorrectionRgba);


			}

		} catch (Throwable t) {
			// Avoid crashing the application due to unhandled exceptions.
			Log.e(TAG, "Exception on the OpenGL thread", t);
		}
	}

	private boolean checkHorizontalPose(Pose hitPose) {
		return (Math.abs(hitPose.qx())< hitEps  && Math.abs(hitPose.qz())< hitEps);
	}

	private void drawTower(float[] colorCorrectionRgba) {
		if (game.towerAnchor!=null){
			game.towerAnchor.getPose().toMatrix(modelMatrix, 0);
			drawMesh(tower.mesh, towerColor, colorCorrectionRgba, modelMatrix, tower.shader);

			float scale = game.tower.radius/game.tower.maxRadius;
			Matrix.scaleM(modelMatrix, 0, scale,1.2f*scale,scale);
			drawMesh(tower.wallMesh, towerColor, colorCorrectionRgba, modelMatrix, tower.wallShader);
		}
	}

	private void drawHelicopter(Camera camera, float[] modelMatrix, float[] colorCorrectionRgba) {
		helicopter.moveRotors(game.frameDuration);
		getHelicopterPosition(camera, modelMatrix);


		drawMesh(helicopter.helicopterMesh, helicopter.color, colorCorrectionRgba, modelMatrix, helicopter.shader);

		Matrix.rotateM(modelMatrix,0, helicopter.rotorAngle , 0,1,0);
		drawMesh(helicopter.helicopterRotorMesh, helicopter.rotorColor, colorCorrectionRgba, modelMatrix,  helicopter.rotorShader);

		Matrix.rotateM(modelMatrix,0, -helicopter.rotorAngle , 0,1,0);
		Matrix.translateM(modelMatrix,0, helicopter.tailRotorTranslate[0], helicopter.tailRotorTranslate[1], helicopter.tailRotorTranslate[2]);
		Matrix.rotateM(modelMatrix,0, helicopter.rotorTailAngle , 1,0,0);

		drawMesh(helicopter.helicopterRotorTailMesh, helicopter.rotorColor, colorCorrectionRgba, modelMatrix, helicopter.rotorShader);
	}

	private void getHelicopterPosition(Camera camera, float[] modelMatrix) {
		//Переместим вертолёт
		Pose sensorPose = camera.getDisplayOrientedPose();

		float qx = sensorPose.qx();
		float qy = sensorPose.qy();
		float qz = sensorPose.qz();
		Pose.makeRotation(0,qy, qz, 1 - qy*qy-qz*qz).inverse().toMatrix(tempMatrix, 0);

		camera.getDisplayOrientedPose().toMatrix(modelMatrix,0);
		//	camera.getPose().extractTranslation().toMatrix(modelMatrix,0);

		Pose.IDENTITY.toMatrix(modelMatrix, 0);
		Matrix.translateM(modelMatrix,0,
				//towerAnchor.getPose().tx(),
				sensorPose.tx(),
				game.towerAnchor.getPose().ty()+0.7f , //Выше плоскости башни на 1 метр
				//towerAnchor.getPose().tz());
				sensorPose.tz());



		Matrix.multiplyMM(modelMatrix,0, modelMatrix, 0, tempMatrix, 0);
		Matrix.rotateM(modelMatrix,0, 180f, 0,1,0);
		Matrix.translateM(modelMatrix,0, 0, 0, 0.4f);
	}

	private void drawMesh(Mesh mesh, float[] color, float[] colorCorrectionRgba, float[] modelMatrix, Shader shader) {

		//Matrix.multiplyMM(scaledModeMatrix, 0, modelMatrix, 0, scaleMatrix, 0);

		// Calculate model/view/projection matrices and view-space light direction
		Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);
		Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, LIGHT_DIRECTION, 0);

		// Update shader properties and draw
		shader
				.setMatrix4("u_ModelView", modelViewMatrix)
				.setMatrix4("u_ModelViewProjection", modelViewProjectionMatrix)
				.set4("u_ColorCorrection", colorCorrectionRgba)
				.set4("u_ViewLightDirection", viewLightDirection)
				.set3("u_AlbedoColor", color);
		render.draw(mesh, shader);
	}

	private void drawTargetMesh(Mesh mesh, float[] color, float[] colorCorrectionRgba, float[] modelMatrix, Shader shader, float time) {

		//Matrix.multiplyMM(scaledModeMatrix, 0, modelMatrix, 0, scaleMatrix, 0);

		// Calculate model/view/projection matrices and view-space light direction
		Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);
		Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, LIGHT_DIRECTION, 0);

		// Update shader properties and draw
		shader
				.setMatrix4("u_ModelView", modelViewMatrix)
				.setMatrix4("u_ModelViewProjection", modelViewProjectionMatrix)
				.set4("u_ColorCorrection", colorCorrectionRgba)
				.set4("u_ViewLightDirection", viewLightDirection)
				.set3("u_AlbedoColor", color)
				.set1("u_time", time);
		render.draw(mesh, shader);
	}

	// Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
	private void handleTap(Frame frame, Camera camera) {
		MotionEvent tap = tapHelper.poll();
		if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
			HitResult hit = hitPosition(frame, camera, tap.getX(), tap.getY());

			if (hit!=null){

				// Adding an Anchor tells ARCore that it should track this position in
				// space. This anchor is created on the Plane to place the 3D model
				// in the correct position relative both to the world and to the plane.
				game.addAnchor(hit.getHitPose());
				//Todo: Kos 14.11.2020 Нужно стрелять из пушек вертолёта
				getHelicopterPosition(camera, modelMatrix);

				game.shot(Pose.makeTranslation(modelMatrix[12],modelMatrix[13], modelMatrix[14]));
				//game.shot(Pose.makeTranslation(sensorPose.tx(),game.towerAnchor.getPose().ty()+0.7f,sensorPose.tz()));

				// For devices that support the Depth API, shows a dialog to suggest enabling
				// depth-based occlusion. This dialog needs to be spawned on the UI thread.
				this.runOnUiThread(this::showOcclusionDialogIfNeeded);

			}
		}
	}

	@Nullable
	private HitResult hitPosition(Frame frame, Camera camera, float x, float y) {
		List<HitResult> hitResultList;
		if (instantPlacementSettings.isInstantPlacementEnabled()) {
			hitResultList =
					frame.hitTestInstantPlacement(x, y, APPROXIMATE_DISTANCE_METERS);
		} else {
			hitResultList = frame.hitTest(x, y);
		}
		for (HitResult hit : hitResultList) {
			// If any plane, Oriented Point, or Instant Placement Point was hit, create an anchor.
			Trackable trackable = hit.getTrackable();
			// If a plane was hit, check that it was hit inside the plane polygon.
			if ((trackable instanceof Plane
					&& ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
					&& (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
					|| (trackable instanceof Point
					&& ((Point) trackable).getOrientationMode()
					== OrientationMode.ESTIMATED_SURFACE_NORMAL)
					|| (trackable instanceof InstantPlacementPoint)) {

				// Hits are sorted by depth. Consider only closest hit on a plane, Oriented Point, or
				// Instant Placement Point.

				return hit;

			}
		}
		return null;
	}

	/**
	 * Shows a pop-up dialog on the first call, determining whether the user wants to enable
	 * depth-based occlusion. The result of this dialog can be retrieved with useDepthForOcclusion().
	 */
	private void showOcclusionDialogIfNeeded() {
		boolean isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
		if (isDepthSupported) {
			depthSettings.setUseDepthForOcclusion(true);
		}
//		if (!depthSettings.shouldShowDepthEnableDialog() || !isDepthSupported) {
//			return; // Don't need to show dialog.
//		}
//
//		// Asks the user whether they want to use depth-based occlusion.
//		new AlertDialog.Builder(this)
//				.setTitle(R.string.options_title_with_depth)
//				.setMessage(R.string.depth_use_explanation)
//				.setPositiveButton(
//						R.string.button_text_enable_depth,
//						(DialogInterface dialog, int which) -> {
//							depthSettings.setUseDepthForOcclusion(true);
//						})
//				.setNegativeButton(
//						R.string.button_text_disable_depth,
//						(DialogInterface dialog, int which) -> {
//							depthSettings.setUseDepthForOcclusion(false);
//						})
//				.show();
	}

	private void launchInstantPlacementSettingsMenuDialog() {
		resetSettingsMenuDialogCheckboxes();
		Resources resources = getResources();
		new AlertDialog.Builder(this)
				.setTitle(R.string.options_title_instant_placement)
				.setMultiChoiceItems(
						resources.getStringArray(R.array.instant_placement_options_array),
						instantPlacementSettingsMenuDialogCheckboxes,
						(DialogInterface dialog, int which, boolean isChecked) ->
								instantPlacementSettingsMenuDialogCheckboxes[which] = isChecked)
				.setPositiveButton(
						R.string.done,
						(DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
				.setNegativeButton(
						android.R.string.cancel,
						(DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
				.show();
	}

	/**
	 * Shows checkboxes to the user to facilitate toggling of depth-based effects.
	 */
	private void launchDepthSettingsMenuDialog() {
		// Retrieves the current settings to show in the checkboxes.
		resetSettingsMenuDialogCheckboxes();

		// Shows the dialog to the user.
		Resources resources = getResources();
		if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
			// With depth support, the user can select visualization options.
			new AlertDialog.Builder(this)
					.setTitle(R.string.options_title_with_depth)
					.setMultiChoiceItems(
							resources.getStringArray(R.array.depth_options_array),
							depthSettingsMenuDialogCheckboxes,
							(DialogInterface dialog, int which, boolean isChecked) ->
									depthSettingsMenuDialogCheckboxes[which] = isChecked)
					.setPositiveButton(
							R.string.done,
							(DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
					.setNegativeButton(
							android.R.string.cancel,
							(DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
					.show();
		} else {
			// Without depth support, no settings are available.
			new AlertDialog.Builder(this)
					.setTitle(R.string.options_title_without_depth)
					.setPositiveButton(
							R.string.done,
							(DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
					.show();
		}
	}

	private void applySettingsMenuDialogCheckboxes() {
		depthSettings.setUseDepthForOcclusion(depthSettingsMenuDialogCheckboxes[0]);
		depthSettings.setDepthColorVisualizationEnabled(depthSettingsMenuDialogCheckboxes[1]);
		instantPlacementSettings.setInstantPlacementEnabled(
				instantPlacementSettingsMenuDialogCheckboxes[0]);
		configureSession();
	}

	private void resetSettingsMenuDialogCheckboxes() {
		depthSettingsMenuDialogCheckboxes[0] = depthSettings.useDepthForOcclusion();
		depthSettingsMenuDialogCheckboxes[1] = depthSettings.depthColorVisualizationEnabled();
		instantPlacementSettingsMenuDialogCheckboxes[0] =
				instantPlacementSettings.isInstantPlacementEnabled();
	}

	/**
	 * Checks if we detected at least one plane.
	 */
	private boolean hasTrackingPlane() {
		for (Plane plane : session.getAllTrackables(Plane.class)) {
			if (plane.getTrackingState() == TrackingState.TRACKING) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns a transformation matrix that when applied to screen space uvs makes them match
	 * correctly with the quad texture coords used to render the camera feed. It takes into account
	 * device orientation.
	 */
	private static float[] getTextureTransformMatrix(Frame frame) {
		float[] frameTransform = new float[6];
		float[] uvTransform = new float[9];
		// XY pairs of coordinates in NDC space that constitute the origin and points along the two
		// principal axes.
		float[] ndcBasis = {0, 0, 1, 0, 0, 1};

		// Temporarily store the transformed points into outputTransform.
		frame.transformCoordinates2d(
				Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
				ndcBasis,
				Coordinates2d.TEXTURE_NORMALIZED,
				frameTransform);

		// Convert the transformed points into an affine transform and transpose it.
		float ndcOriginX = frameTransform[0];
		float ndcOriginY = frameTransform[1];
		uvTransform[0] = frameTransform[2] - ndcOriginX;
		uvTransform[1] = frameTransform[3] - ndcOriginY;
		uvTransform[2] = 0;
		uvTransform[3] = frameTransform[4] - ndcOriginX;
		uvTransform[4] = frameTransform[5] - ndcOriginY;
		uvTransform[5] = 0;
		uvTransform[6] = ndcOriginX;
		uvTransform[7] = ndcOriginY;
		uvTransform[8] = 1;

		return uvTransform;
	}

	private static Shader createTargetShader(
			SampleRender render, Texture virtualObjectTexture, boolean useDepthForOcclusion)
			throws IOException {
		return Shader.createFromAssets(
				render,
				"shaders/target.vert",
				"shaders/target.frag",
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

	private static Shader createPowerShader(
			SampleRender render, Texture virtualObjectTexture, boolean useDepthForOcclusion)
			throws IOException {
		return Shader.createFromAssets(
				render,
				"shaders/power.vert",
				"shaders/power.frag",
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

	/**
	 * Configures the session with feature settings.
	 */
	private void configureSession() {
		Config config = session.getConfig();
		if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
			config.setDepthMode(Config.DepthMode.AUTOMATIC);
		} else {
			config.setDepthMode(Config.DepthMode.DISABLED);
		}
		if (instantPlacementSettings.isInstantPlacementEnabled()) {
			config.setInstantPlacementMode(InstantPlacementMode.LOCAL_Y_UP);
		} else {
			config.setInstantPlacementMode(InstantPlacementMode.DISABLED);
		}
		session.configure(config);
	}

	/**
	 * Assign a color to the object for rendering based on the trackable type this anchor attached to.
	 * For AR_TRACKABLE_POINT, it's blue color.
	 * For AR_TRACKABLE_PLANE, it's green color.
	 * For AR_TRACKABLE_INSTANT_PLACEMENT_POINT while tracking method is
	 * SCREENSPACE_WITH_APPROXIMATE_DISTANCE, it's white color.
	 * For AR_TRACKABLE_INSTANT_PLACEMENT_POINT once tracking method becomes FULL_TRACKING, it's
	 * orange color.
	 * The color will update for an InstantPlacementPoint once it updates its tracking method from
	 * SCREENSPACE_WITH_APPROXIMATE_DISTANCE to FULL_TRACKING.
	 */
	private float[] getTrackableColor(Trackable trackable) {
		if (trackable instanceof Point) {
			return new float[]{66.0f / 255.0f, 133.0f / 255.0f, 244.0f / 255.0f};
		}
		if (trackable instanceof Plane) {
			return new float[]{139.0f / 255.0f, 195.0f / 255.0f, 74.0f / 255.0f};
		}
		if (trackable instanceof InstantPlacementPoint) {
			if (((InstantPlacementPoint) trackable).getTrackingMethod()
					== TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE) {
				return new float[]{255.0f / 255.0f, 255.0f / 255.0f, 255.0f / 255.0f};
			}
			if (((InstantPlacementPoint) trackable).getTrackingMethod() == TrackingMethod.FULL_TRACKING) {
				return new float[]{255.0f / 255.0f, 167.0f / 255.0f, 38.0f / 255.0f};
			}
		}
		// Fallback color.
		return new float[]{0f, 0f, 0f};
	}
}
