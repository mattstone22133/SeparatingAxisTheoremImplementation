package sat.simulation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

public class WrappedCubeModel
{
	private ModelBuilder modelBuilder;
	private Model cubeGreenArchType;
	private float cubeWidth;
	private float cubeHeight;
	private float cubeDepth;
	
	private static Model redSphereArchType = null;
	private static ModelInstance redSphere = null;

	public WrappedCubeModel(float cubeWidth, float cubeHeight, float cubeDepth, Color modelColor)
	{
		this.modelBuilder = new ModelBuilder();
		
		//lazy initialization
		if(WrappedCubeModel.redSphereArchType == null)
		{
			float sphereSize = 0.2f;
			WrappedCubeModel.redSphereArchType = modelBuilder.createSphere(sphereSize, sphereSize, sphereSize,
					10, 10, 
					new Material(ColorAttribute.createDiffuse(Color.RED)),
					VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
					0, 360f,
					0, 360f);
			
			redSphere = new ModelInstance(redSphereArchType);
		}
		
		this.cubeWidth = cubeWidth;
		this.cubeHeight = cubeHeight;
		this.cubeDepth = cubeDepth;

		cubeGreenArchType = modelBuilder.createBox(cubeWidth, cubeHeight, cubeDepth, new Material(ColorAttribute.createDiffuse(modelColor)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
	}
	
	public CubeInstance getInstance()
	{
		return new CubeInstance(cubeWidth, cubeHeight, cubeDepth, new ModelInstance(cubeGreenArchType));
	}
	
/// ----------------------------------------------------------------------------------------------------- //
	public class CubeInstance
	{
		public final float cubeWidth;
		public final float cubeHeight;
		public final float cubeDepth;
		private ModelInstance cube;
		private Vector3[] vertices;
		private Vector3[] transformedVertices;

		private CubeInstance(float cubeWidth, float cubeHeight, float cubeDepth, ModelInstance cube)
		{
			this.cubeWidth = cubeWidth;
			this.cubeHeight = cubeHeight;
			this.cubeDepth = cubeDepth;
			this.cube = cube;
			this.vertices = new Vector3[8];
			this.transformedVertices = new Vector3[8];
			
			for(int i = 0; i < 8; ++i)
			{
				vertices[i] = new Vector3();
				transformedVertices[i] = new Vector3();
			}
			
			//front face 
			vertices[0].set(cubeDepth/2, cubeHeight/2, cubeDepth/2); //top right
			vertices[1].set(-cubeDepth/2, cubeHeight/2, cubeDepth/2); //top left
			vertices[2].set(-cubeDepth/2, -cubeHeight/2, cubeDepth/2); //bottom left
			vertices[3].set(cubeDepth/2, -cubeHeight/2, cubeDepth/2); //bottom right
			
			//rear face
			vertices[4].set(cubeDepth/2, cubeHeight/2, -cubeDepth/2); //top right
			vertices[5].set(-cubeDepth/2, cubeHeight/2, -cubeDepth/2); //top left
			vertices[6].set(-cubeDepth/2, -cubeHeight/2, -cubeDepth/2); //bottom left
			vertices[7].set(cubeDepth/2, -cubeHeight/2, -cubeDepth/2); //bottom right
		}
		
		public void render(ModelBatch modelBatch, Environment environment)
		{
			//assuming model batch started
			modelBatch.render(cube, environment);
			
			for(Vector3 vertex : getTransformVertices())
			{
				//render red sphere at each vertice
				redSphere.transform.setTranslation(vertex);
				modelBatch.render(redSphere, environment);
			}
			
		}
		
		public Vector3[] getTransformVertices()
		{
			for(int i = 0; i < vertices.length; ++i)
			{
				Vector3 vertex = vertices[i];
				transformedVertices[i].set(vertex);
				transformedVertices[i].mul(cube.transform);
			}
			
			return transformedVertices;
		}

		public ModelInstance model()
		{
			return cube;
		}
	}
}
