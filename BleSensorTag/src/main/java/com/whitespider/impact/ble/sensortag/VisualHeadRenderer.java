package com.whitespider.impact.ble.sensortag;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

public class VisualHeadRenderer extends ApplicationAdapter {
	private PerspectiveCamera camera;
	private ModelBatch modelBatch;
	private ModelInstance boxInstance;
	private Environment environment;
	private int dragX, dragY;
	private float lightIntensity = 1f;
	private PointLight pointLight;
	private ModelInstance arrowInstance;
	private Model head;
	private Model arrow;

	@Override
	public void create () {
		camera = new PerspectiveCamera(
				75,
				Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());

		// Move the camera 3 units back along the z-axis and look at the origin
		camera.position.set(.7f,0.2f,0.1f);
		camera.lookAt(0f, 0f, 0f);

		// Near and Far (plane) repesent the minimum and maximum ranges of the camera in, um, units
		camera.near = 0.1f;
		camera.far = 30.0f;

		modelBatch = new ModelBatch();
		ModelBuilder modelBuilder = new ModelBuilder();
		Model box = createHead();
		boxInstance = new ModelInstance(box);
		Model arrow = createArrow();
		arrowInstance = new ModelInstance(arrow);

		//SpotLight spotLight = new SpotLight();
		//spotLight.direction = new Vector3()


		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1.0f));
		//environment.set(new ColorAttribute(ColorAttribute.createDiffuse(Color.GREEN)));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
//		environment.add(new SpotLight().set(Color.RED,
//				new Vector3(0.8f, 0.8f, 0.8f), new Vector3(1.6f, 1.6f, 1.6f), 200, 1, 7));
		//environment.add(new PointLight().set(Color.RED, new Vector3(0f, 1.0f, 0.0f), 2));
		//environment.add(new SpotLight().set(Color.RED, new Vector3(0f, 0f, 0f), new Vector3(1f, 1f, 1f), 20, 1, 7));

		Gdx.input.setInputProcessor(new InputProcessor() {
			@Override
			public boolean keyDown(int keycode) {
				return false;
			}

			@Override
			public boolean keyUp(int keycode) {
				return false;
			}

			@Override
			public boolean keyTyped(char character) {
				return false;
			}

			@Override
			public boolean touchUp(int screenX, int screenY, int pointer, int button) {
				return false;
			}

			@Override
			public boolean touchDragged(int screenX, int screenY, int pointer) {
				float dX = (float) (screenX - dragX) / (float) Gdx.graphics.getWidth();
				float dY = (float) (dragY - screenY) / (float) Gdx.graphics.getHeight();
				dragX = screenX;
				dragY = screenY;
				camera.rotateAround(Vector3.Zero, new Vector3(0, 1, 0), dX > 0 ? -2f : +2f);
				//environment.remove(pointLight);
				//pointLight = new PointLight().set(Color.RED, camera.position, lightIntensity);
				//environment.add(pointLight);
				camera.update();
				return true;
			}

			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				dragX = screenX;
				dragY = screenY;
				return true;
			}

			@Override
			public boolean mouseMoved(int screenX, int screenY) {
				return false;
			}

			@Override
			public boolean scrolled(int amount) {
				return false;
			}
		});
	}

	private Model createHead() {
		ModelLoader loader = new ObjLoader();
		Model model = loader.loadModel(Gdx.files.internal("data/MaleHead-Free_TurboSquid.obj"));
		return model;
	}
	private Model createArrow() {
		Vector3 from = new Vector3(0.5f,0.5f,0);
		Vector3 to = new Vector3(0.2f,0.2f,0);
		ModelBuilder modelBuilder = new ModelBuilder();
		Model arrow = modelBuilder.createArrow(from, to,
				new Material(ColorAttribute.createDiffuse(Color.RED)),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
		);

		return arrow;
	}
	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

//		camera.rotateAround(Vector3.Zero, new Vector3(0, 1, 0), 1f);
		camera.update();

		modelBatch.begin(camera);
		modelBatch.render(boxInstance, environment);
		modelBatch.render(arrowInstance, environment);
		modelBatch.end();
	}

	@Override
	public void resume() {
		super.resume();
	}

	@Override
	public void dispose() {
		super.dispose();
		modelBatch.dispose();
		//head.dispose();
		//arrow.dispose();
	}
}
