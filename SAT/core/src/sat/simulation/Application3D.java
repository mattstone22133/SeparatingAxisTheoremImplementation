package sat.simulation;

import java.nio.FloatBuffer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import sat.simulation.WrappedCubeModel.CubeInstance;

public class Application3D extends ApplicationAdapter implements InputProcessor
{
	// 3D classes provided with Libgdx framework
	private ModelBatch modelBatch;
	private ModelBuilder modelBuilder;
	private Model cubeGreenArchType;
	private ModelInstance cube;
	private Environment environment;
	private DirectionalLight mainLight;
	private PerspectiveCamera camera;
	private FirstPersonCameraController fpc;

	// text
	private BitmapFont bmFont;
	private SpriteBatch spriteBatch;

	// logic
	private float translationSpeed = 5f;
	private float rotationSpeed = 5f;

	private boolean collisionLibraryDetected;
	private boolean collisionSATDetected;

	private SAT.RenderInformation2D renderInfo;
	private boolean useMTV = false;
	private Vector2 mtv = new Vector2();
	private CubeInstance cubeBlue;

	@Override
	public void create()
	{
		camera = new PerspectiveCamera(75, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.near = 0.01f;
		camera.far = 500f;
		camera.position.set(10, 0, 15);
		camera.lookAt(0, 0, 0);
		fpc = new FirstPersonCameraController(camera);
		fpc.setVelocity(10);
		Gdx.input.setInputProcessor(this);

		mainLight = new DirectionalLight();
		mainLight.set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1f));
		environment.add(mainLight);

		modelBatch = new ModelBatch();
		modelBuilder = new ModelBuilder();

		float cubeWidth = 3, cubeHeight = 3, cubeDepth = 3;
		// cubeGreenArchType = modelBuilder.createBox(cubeWidth, cubeHeight, cubeDepth,
		// new Material(ColorAttribute.createDiffuse(Color.GREEN)),
		// VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		// cube = new ModelInstance(cubeGreenArchType);

		WrappedCubeModel cubeGreenArchtype = new WrappedCubeModel(cubeWidth, cubeHeight, cubeDepth, Color.GREEN);
		cubeGreen = cubeGreenArchtype.getInstance();

		WrappedCubeModel cubeBlueArchtype = new WrappedCubeModel(cubeWidth, cubeHeight, cubeDepth, Color.BLUE);
		cubeBlue = cubeBlueArchtype.getInstance();
		cubeBlue.model().transform.setTranslation(10f, 0f, 0f);

		bmFont = new BitmapFont();
		bmFont.setColor(Color.WHITE);
		spriteBatch = new SpriteBatch();

	}

	@Override
	public void render()
	{
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		camera.update();

		keyboard_IO();
		calculateCollisions();

		// FloatBuffer verticesBuffer = cube.model.meshParts.get(0).mesh.getVerticesBuffer();
		// FloatBuffer verticesBuffer = cube.model.meshes.get(0).getVerticesBuffer();
		//
		//// float[] array = verticesBuffer.array();//unsupported operation exception
		// for(int i =0; i < verticesBuffer.limit(); ++i)
		// {
		// System.out.println(verticesBuffer.get(i));
		// }

		modelBatch.begin(camera);
		// modelBatch.render(cube, environment);
		cubeGreen.render(modelBatch, environment);
		cubeBlue.render(modelBatch, environment);
		modelBatch.end();

		renderText();
	}

	private void renderText()
	{
		spriteBatch.begin();
		if (collisionLibraryDetected)
		{
			// bmFont.draw(batch, "Library detected collision", 10f, 0 + 2 * bmFont.getCapHeight());
		}
		if (collisionSATDetected)
		{
			// bmFont.draw(batch, "SAT detected collision", Gdx.graphics.getWidth() / 2 - 10f, 0 + 2
			// * bmFont.getCapHeight());
		}
		else
		{
			// bmFont.draw(batch, "No detected collisions", Gdx.graphics.getWidth() / 2
			// - bmFont.getSpaceWidth() * "No detected collisions".length() //approximately center
			// this text
			// ,0 + 2 * bmFont.getCapHeight());
		}
		if (useMTV)
		{
			// bmFont.draw(batch, "MTV enabled", Gdx.graphics.getWidth() * 0.45f,
			// Gdx.graphics.getHeight() * 0.80f);
		}

		spriteBatch.end();
	}

	private void calculateCollisions()
	{
		// check collision via library methods
		// collisionLibraryDetected = Intersector.overlapConvexPolygons(square, triangle);

		if (!useMTV)
		{
			// collisionSATDetected = SAT.PolygonCollide_2D_v1(renderInfo,
			// square.getTransformedVertices(), triangle.getTransformedVertices());
		}
		else
		{
			// collisionSATDetected = SAT.PolygonCollide_2D_mtv(renderInfo,
			// square.getTransformedVertices(), triangle.getTransformedVertices(), mtv);
			if (collisionSATDetected)
			{
				// square.translate(mtv.x, mtv.y);
			}
		}
	}

	private static Vector3 rotatePnt = new Vector3(0f, 0f, 0f);
	private static Vector3 xAxis = new Vector3(1f, 0f, 0f);
	private static Vector3 yAxis = new Vector3(0f, 1f, 0f);
	private static Vector3 zAxis = new Vector3(0f, 0f, 1f);
	private static float rotationDegrees = 1f;
	private CubeInstance cubeGreen;

	private void keyboard_IO()
	{
		float rotationDegrees = 2f;
		fpc.update();
		
		if (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT)) 
		{
			//CONTROL IS FOR ROTATING
			if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))
			{
				if (Gdx.input.isKeyPressed(Input.Keys.UP))
				{
					cubeBlue.model().transform.rotate(xAxis, rotationDegrees);
				}
				else if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
				{
					cubeBlue.model().transform.rotate(yAxis, rotationDegrees);
				}
				else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
				{
					cubeBlue.model().transform.rotate(yAxis, -rotationDegrees);
				}
				else if (Gdx.input.isKeyPressed(Input.Keys.DOWN))
				{
					cubeBlue.model().transform.rotate(xAxis, -rotationDegrees);
				}
			}
			else if (Gdx.input.isKeyPressed(Input.Keys.UP))
			{
				// camera.rotateAround(rotatePnt, xAxis, rotationDegrees);
				cubeBlue.model().transform.translate(0, 0, -0.1f);
			}
			else if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
			{
				cubeBlue.model().transform.translate(-0.1f, 0f, 0f);
			}
			else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
			{
				cubeBlue.model().transform.translate(0.1f, 0f, 0f);
			}
			else if (Gdx.input.isKeyPressed(Input.Keys.DOWN))
			{
				cubeBlue.model().transform.translate(0, 0, 0.1f);
			}
		}
		else
		{
			//CONTROL IS FOR ROTATING
			if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))
			{
				if (Gdx.input.isKeyPressed(Input.Keys.UP))
				{
					cubeGreen.model().transform.rotate(xAxis, 2f);
				}
				else if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
				{
					cubeGreen.model().transform.rotate(xAxis, -2f);
				}
				else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
				{
					cubeGreen.model().transform.rotate(yAxis, -2f);
				}
				else if (Gdx.input.isKeyPressed(Input.Keys.DOWN))
				{
					cubeGreen.model().transform.rotate(yAxis, -2f);
				}
			}
			else if (Gdx.input.isKeyPressed(Input.Keys.UP))
			{
				// camera.rotateAround(rotatePnt, xAxis, rotationDegrees);
				cubeGreen.model().transform.translate(0, 0, -0.1f);
			}
			else if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
			{
				cubeGreen.model().transform.translate(-0.1f, 0f, 0f);
			}
			else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
			{
				cubeGreen.model().transform.translate(0.1f, 0f, 0f);
			}
			else if (Gdx.input.isKeyPressed(Input.Keys.DOWN))
			{
				cubeGreen.model().transform.translate(0, 0, 0.1f);
			}
		}

		if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
		{
			Gdx.app.exit();
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.M))
		{
			useMTV = !useMTV;
		}
	}

	@Override
	public void dispose()
	{
		bmFont.dispose();
		spriteBatch.dispose();
		modelBatch.dispose();

	}

	// ----------------------------- INPUT PROCESSOR METHODS -----------------------------

	@Override
	public boolean keyDown(int keycode)
	{
		return fpc.keyDown(keycode);
	}

	@Override
	public boolean keyUp(int keycode)
	{
		return fpc.keyUp(keycode);
	}

	@Override
	public boolean keyTyped(char character)
	{
		return fpc.keyTyped(character);
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button)
	{
		return fpc.touchDown(screenX, screenY, pointer, button);
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button)
	{
		return fpc.touchUp(screenX, screenY, pointer, button);
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer)
	{
		return fpc.touchDragged(screenX, screenY, pointer);
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY)
	{
		return fpc.mouseMoved(screenX, screenY);
	}

	@Override
	public boolean scrolled(int amount)
	{
		return fpc.scrolled(amount);
	}
}
