package sat.simulation;

import java.util.ArrayList;
import java.util.Stack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

/**
 * Class responsible for Separating Axis Theorem calculations.
 * 
 * @author Matt Stone
 * @author Brenner Harris
 *
 */
/**
 * @author matt
 *
 */
public class SAT
{
	private static Stack<Segment2D> recycledSegments = new Stack<Segment2D>();
	private static ArrayList<Segment2D> obj1Vectors = new ArrayList<Segment2D>();
	private static ArrayList<Segment2D> obj2Vectors = new ArrayList<Segment2D>();

	/**
	 * SAT on 2D polygon. Assumes two sets of vertices can be used to provide a line segment surface
	 * from which a normal vector can be determined.
	 * 
	 * @param startedShapeRenderer
	 * 
	 * @param obj1Vertices
	 * @param obj2Vertices
	 * @return
	 */
	public static boolean PolygonCollide_2D_v1(ShapeRenderer startedShapeRenderer, float[] obj1Vertices, float[] obj2Vertices)
	{
		// determine normal vectors, these will be the axes
		segmentize(obj1Vertices, obj1Vectors);
		segmentize(obj2Vertices, obj2Vectors);
		convertSegmentsToNormals(obj1Vectors);
		convertSegmentsToNormals(obj2Vectors);

		if(startedShapeRenderer != null)
		{
			renderAxes(startedShapeRenderer, obj1Vectors, Gdx.graphics.getWidth() * 0.33f);
			renderAxes(startedShapeRenderer, obj2Vectors, Gdx.graphics.getWidth() * 0.66f);
		}
		
		// project min / max vertices on axes
		boolean nonCollisionFound = false;
		for(Segment2D axis : obj1Vectors)
		{
			// test whether projections overlap. 
			// if there is a non-overlapping projection, there cannot be a collision. 
			nonCollisionFound |= !projectionOverlap(obj1Vertices, obj2Vertices, axis);
		}
		for(Segment2D axis : obj2Vectors)
		{
			nonCollisionFound |= !projectionOverlap(obj1Vertices, obj2Vertices, axis);
		}
		
		// clean up resources
		cleanUpRecycledResources();

		return !nonCollisionFound;
	}

	private static void renderAxes(ShapeRenderer startedShapeRenderer, ArrayList<Segment2D> normals, float offset)
	{
		//float offset = Gdx.graphics.getWidth() / 2;
		float scale = 100;
		
		startedShapeRenderer.begin(ShapeType.Line);
		startedShapeRenderer.setColor(1, 1, 1, 1);
		
		for(Segment2D segment : normals)
		{
			Segment2D axis = segment;
			startedShapeRenderer.line(-scale * axis.firstVertX  + offset,
					-scale * axis.secondVertY + offset, 
					scale * axis.firstVertX + offset,
					scale * axis.secondVertY + offset);
		}
		startedShapeRenderer.end();
	}

	/**
	 * Converts vertices into line segments (see helper struct Segment2D)
	 * 
	 * @param objVertices
	 * @param segmentContainer a container that contains all of an objects 2D segments.
	 */
	private static void segmentize(float[] objVertices, ArrayList<Segment2D> segmentContainer)
	{
		for (int i = 0; i < objVertices.length; i += 2)
		{
			Segment2D segment = getNewSegment();
			segment.firstVertX = objVertices[i];
			segment.firstVertY = objVertices[i + 1];
			if (i + 2 < objVertices.length)
			{
				// use the next vertex as the connecting point
				segment.secondVertX = objVertices[i + 2];
				segment.secondVertY = objVertices[i + 3];
			}
			else
			{
				// wrap around and use the first vertex as the starting point
				segment.secondVertX = objVertices[0];
				segment.secondVertY = objVertices[1];
			}
			segmentContainer.add(segment);
		}
	}
	
	/**
	 * Converts each segment into its normal vector. A 2D segment is defined by two vectors. 
	 * The vector difference between a segments vectors gives a vector that lies on the plane that both vectors belong to.
	 * 
	 * With a vector that lies on the plane (ie line in 2d) defined by a segment, we can find the normal vector. 
	 * The dot product between the vector within the plane and the normal vector should equal zero (They're perpendicular).
	 * We can use linear algebra to solve for the normal vector. 
	 * 
	 * @param segments
	 */
	private static void convertSegmentsToNormals(ArrayList<Segment2D> segments)
	{
		for (int i = 0; i < segments.size(); ++i)
		{
			Segment2D segment = segments.get(i);
			
			//take the vector difference to derive the vector within the plane (i.e. the line since we're in 2D). 
			float planeX = segment.secondVertX - segment.firstVertX;
			float planeY = segment.secondVertY - segment.firstVertY;
			
			// (normal) dot (plane) = 0
			// normalX(planeX) + normalY(planeY) = 0
			// for every unknown except 1, assign a value
			
			// ALGEBRA
			// normalX = 1
			// 1*(planeX) + normalY*(planeY) = 0
			// normalY*planeY = -(1 * planeX)
			// normalY = -(1 * planeX) / planeY //adjust for zero division, both cannot be 0; choose to divide by that which is not 0
			
			float arbitraryValue = 1;
			float normalX, normalY = 0;
			if(planeY != 0)
			{
				normalX = arbitraryValue;
				normalY = -(normalX * planeX) / planeY;
			}
			else
			{
				normalY = arbitraryValue;
				normalX = -(normalY * planeY) / planeX;
			}
			segment.firstVertX = segment.secondVertX = normalX;
			segment.firstVertY = segment.secondVertY = normalY;
		}
	}
	
	
	private static boolean projectionOverlap(float[] obj1Vertices, float[] obj2Vertices, Segment2D axis)
	{
		float obj1Min = Float.POSITIVE_INFINITY, obj2Min = Float.POSITIVE_INFINITY;
		float obj1Max = Float.NEGATIVE_INFINITY, obj2Max = Float.NEGATIVE_INFINITY;
		
		float vDotV = dot2D(axis.firstVertX, axis.firstVertY, axis.firstVertX, axis.firstVertY);
		for(int i = 0; i < obj1Vertices.length; i += 2)
		{
			float projX = 0, projY = 0;
			
			//line can be interpreted as C*(vector_on_line).
			//the projection on the line/axis, can be said to be a specific value of C*vector.
			//A right triangle can be made between the axis vector (which is mult by C) and the vector we're projecting onto the axis.
			//The base of the triangle (ie the axis) and the height (ie the base - projected_vector) are orthogonal to each other, this means their dot is 0.
			//However, we don't know the height of the triangle. But it can be said to be the vector difference of the projection vector with the axis vector.
			//ie projectionVector - axis vector. 
			//We derive the following equation: (projected - c*v) DOT (V) = 0 -- where projected is the projected vector, c*v is the axis, and v is the vector defining the axis
			//The following is an algebraic manipulation for solving for C.
			//(projectedVect -c*v) DOT (V) = 0
			//(projectedVect DOT V) - (c*V DOT V) = 0
			//(projectedVect DOT V) = -(c*V DOT V)
			//(projectedVect DOT V) / (V DOT V) = C
			float C = dot2D(obj1Vertices[i], obj1Vertices[i+1], axis.firstVertX, axis.firstVertY) / vDotV;
			projX = C * axis.firstVertX;
			projY = C * axis.secondVertY;
			
			//float projMagnitude = (float) Math.sqrt(Math.pow(projX, 2) + Math.pow(projY, 2)); I think we can just compare C to determine min/max of project
			if(C < obj1Min)
			{
				obj1Min = C;
			}
			if(C > obj1Max)
			{
				obj1Max = C;
			}
		}
		for(int i = 0; i < obj2Vertices.length; ++i)
		{
			
		}
		return false;
	}


	/**
	 * Returns all used segments to the static scope global recycled container. Clears out the array
	 * that stores segments. This helps by preventing new object allocation every time the SAT
	 * algorithm is called.
	 */
	private static void cleanUpRecycledResources()
	{
		for (Segment2D segment : obj1Vectors)
		{
			recycleSegment(segment);
		}
		for (Segment2D segment : obj2Vectors)
		{
			recycleSegment(segment);
		}
		obj1Vectors.clear();
		obj2Vectors.clear();
	}

	/**
	 * Get a new segment to use. This pulls from a recycle bin if there are available segments.
	 * Otherwise it creates a new segment. The clean up method should be called after this method is
	 * used to return segments to the recycle bin. Using a recycle bin prevents the creation of new
	 * objects every time this method is called.
	 * 
	 * @return
	 */
	private static Segment2D getNewSegment()
	{
		if (!recycledSegments.isEmpty())
		{
			return recycledSegments.pop();
		}
		else
		{
			return new Segment2D();
		}
	}

	/**
	 * Returns a segment to the recycle bin.
	 * 
	 * @param segment the segment that is no longer being used.
	 */
	private static void recycleSegment(Segment2D segment)
	{
		if (segment != null)
		{
			recycledSegments.push(segment);
		}
	}

	/**
	 * A simple struct that represents a 2D segment defined by a start and end point.
	 * 
	 * @author Matt Stone
	 *
	 */
	protected static class Segment2D
	{
		public float firstVertX = 0;
		public float firstVertY = 0;
		public float secondVertX = 0;
		public float secondVertY = 0;
	}
	
	public static float dot2D(float x1, float y1, float x2, float y2)
	{
		
	}
}
