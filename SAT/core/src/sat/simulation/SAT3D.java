package sat.simulation;

import java.util.ArrayList;
import java.util.Stack;

import com.badlogic.gdx.math.Vector3;

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
public class SAT3D
{
	private static Stack<Segment3D> recycledSegments = new Stack<Segment3D>();
	private static ArrayList<Segment3D> cube1Vectors = new ArrayList<Segment3D>();
	private static ArrayList<Segment3D> cube2Vectors = new ArrayList<Segment3D>();
	private static ArrayList<Vector3> axes = new ArrayList<Vector3>();
	public static Vector3 tempBuffer = new Vector3();

	public static boolean CubeCollide_3D_mtv(Vector3[] obj1Vertices, Vector3[] obj2Vertices, Vector3 mtvBuffer)
	{
		// determine normal vectors, these will be the axes
		segmentizeCube(obj1Vertices, cube1Vectors);
		segmentizeCube(obj2Vertices, cube2Vectors);
		
		convertSegmentsToAxes(cube1Vectors, cube2Vectors, axes);

		mtvBuffer.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

		// project min / max vertices on axes
		// NOTE: boolean redundant checks because this method draws the projections, must check
		// every projection to draw it.
		boolean collision = true;
		for (Vector3 axis : axes)
		{
			// test whether projections overlap.
			// if there is a non-overlapping projection, there cannot be a collision.
			collision &= projectionOverlap_MTV(obj1Vertices, obj2Vertices, axis, tempBuffer);
			
			float magnitudeOfBuffer = vect1IsMinimumMagnitude(tempBuffer, mtvBuffer);
			// zero signals the new translation vector is not smaller than last.
			if (magnitudeOfBuffer != 0)
			{
				mtvBuffer.set(tempBuffer);
			}
			
			//early exit if we find we're not coliding
			if(!collision)
			{
				mtvBuffer.set(0,0,0);
				break;
			}
		}
		
		// clean up resources
		cleanUpRecycledResources();
		return collision;
	}

	private static float vect1IsMinimumMagnitude(Vector3 vect1, Vector3 vect2)
	{
		float pnt1Length = vect1.len();
		float pnt2Length = vect2.len();

		if (pnt1Length < pnt2Length) { return pnt1Length; }
		// signals false, use int return types to allow use of non-zero returns to update length.
		return 0f;
	}

	private final static float constantOffset = 0.001f;
	private static boolean projectionOverlap_MTV(Vector3[] obj1Vertices, Vector3[] obj2Vertices, Vector3 vectorOnAxis, Vector3 mtv)
	{
		float obj1Min = Float.POSITIVE_INFINITY, obj2Min = Float.POSITIVE_INFINITY;
		float obj1Max = Float.NEGATIVE_INFINITY, obj2Max = Float.NEGATIVE_INFINITY;

		float vDotV = vectorOnAxis.dot(vectorOnAxis);
		for(Vector3 vertexObj1 : obj1Vertices)
		{

			// line/axis can be interpreted as C*(vector_on_line).
			// the projection on the line/axis, can be said to be a specific value of C multiplied by the vector.
			// A right triangle can be made between the axis vector (which is mult by C) and the
			// vector we're projecting onto the axis.
			//
			// The base of the triangle (ie the axis) and 
			// the height (ie the base - projected_vector)
			// are orthogonal to each other, this means their dot is 0.
			//
			// However, we don't know the height of the triangle. But it can be said to be the
			// vector difference of the projection vector with the axis vector.
			// ie height vector = projectionVector - axis vector.
			//
			// We derive the following equation: (projected_from - c*v) DOT (V) = 0 
			// -- where projected is the projected vector, c*v is the resulting projection, and v is the vector defining the axis
			//
			// The following is an algebraic manipulation for solving for C.
			// (projectedVect -c*v) DOT (V) = 0
			// (projectedVect DOT V) - (c*V DOT V) = 0
			// (projectedVect DOT V) = -(c*V DOT V)
			// (projectedVect DOT V) / (V DOT V) = C
			float C = vertexObj1.dot(vectorOnAxis) / vDotV;

			// float projX = 0, projY = 0;
			// projX = C * axis.firstVertX;
			// projY = C * axis.secondVertY;

			// float projMagnitude = (float) Math.sqrt(Math.pow(projX, 2) + Math.pow(projY, 2)); I
			// think we can just compare C to determine min/max of project
			if (C < obj1Min)
			{
				obj1Min = C;
			}
			if (C > obj1Max)
			{
				obj1Max = C;
			}
		}
		//for (int i = 0; i < obj2Vertices.length; i += 2)
		for(Vector3 vertexObj2 : obj2Vertices)
		{
			float C = vertexObj2.dot(vectorOnAxis) / vDotV;
			// float projX = 0, projY = 0;
			// projX = C * axis.firstVertX;
			// projY = C * axis.secondVertY;

			// float projMagnitude = (float) Math.sqrt(Math.pow(projX, 2) + Math.pow(projY, 2)); I
			// think we can just compare C to determine min/max of project
			if (C < obj2Min)
			{
				obj2Min = C;
			}
			if (C > obj2Max)
			{
				obj2Max = C;
			}
		}

		//@formatter:off
		//Imagine the objMin/Max ranges as being segments on a the X-axis.
		//in reality, they represent scalars to multiply against the true axis. 
		boolean obj1MaxOverlapsObj2Min = obj1Max >= obj2Min && obj1Min <= obj2Min;
		boolean obj2MaxOverlapsObj1Min = obj2Max >= obj1Min && obj2Min <= obj1Min;
		boolean obj1ContainsObj2 = obj1Max >= obj2Max && obj2Min >= obj1Min;
		boolean obj2ContainsObj1 = obj2Max >= obj1Max && obj1Min >= obj2Min;
		
		if ((obj1MaxOverlapsObj2Min) // overlap at obj1max and obj2min
				|| (obj2MaxOverlapsObj1Min) // overlap at obj2max and obj1min
				|| (obj1ContainsObj2) // 1 contains 2
				|| (obj2ContainsObj1) // 2 contains 1
				)
		{ //@formatter:on
			// the above assumes obj1 is the moving object.
			if (mtv != null)
			{
				// Correct obj1's position with a translation vector. This will be the vector to
				// remove obj1 from collision
				// The translation vector to correct collision will be along this axis.
				// therefore, we need to find a constant to multiply the axis by to find the
				// translation vector.
				float C = 1; // this is the constant by which to modify the axis vector
				if (obj1MaxOverlapsObj2Min)
				{
					// need vector to point towards minimum of obj2; as if obj1 bumped into obj1
					C = obj2Min - obj1Max;
				}
				else if (obj2MaxOverlapsObj1Min)
				{
					// vector should point point towards obj2 max; as if obj1 "backed" into obj2
					C = obj2Max - obj1Min;
				}
				else // if (obj1ContainsObj2 || obj2ContainsObj1) //turns out logic is the same for
						// both cases.
				{
					// unclear which direction to move obj1 without having a reference from where
					// obj1 is moving.
					// assume segment vertices that are closest represent direction coming from.
					// <with the translation vector that lead to condition, we could find direction
					// to correct with>
					if (Math.abs(obj1Max - obj2Max) > Math.abs(obj1Min - obj2Min))
					{
						// move in direction of max
						// must move obj1's min passed obj2's max
						C = obj2Max - obj1Min;
					}
					else
					{
						// move in direction of min
						// move obj1's max passed obj2's min
						C = obj2Min - obj1Max;
					}
				}

				// this gives the vector a slight *nudge*.
				// bug occured where there would be 2 MTVs used in 2 iterations. 1 moved the MTV
				// this is related to using > vs. >= when comparing projections. While doing >
				// fixes a sudden large move, it doesn't appear as smooth as adding a small nudge.
				C = C > 0 ? C + constantOffset : C - constantOffset; // branching does have a
																		// potential slowdown w/
																		// mispredictions; tradeoff:
																		// looks(jittery) vs. speed

				// take difference of segments, use difference to determine overlap
				// use overlap to construct a vector. Vector should affect object 1
				mtv.set(vectorOnAxis);
				mtv.z *= C;
				mtv.y *= C;
				mtv.x *= C;
			}
			return true;
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
		for (Segment3D segment : cube1Vectors)
		{
			recycleSegment(segment);
		}
		for (Segment3D segment : cube2Vectors)
		{
			recycleSegment(segment);
		}
		cube1Vectors.clear();
		cube2Vectors.clear();
	}

	/**
	 * Get a new segment to use. This pulls from a recycle bin if there are available segments.
	 * Otherwise it creates a new segment. The clean up method should be called after this method is
	 * used to return segments to the recycle bin. Using a recycle bin prevents the creation of new
	 * objects every time this method is called.
	 * 
	 * @return
	 */
	private static Segment3D getNewSegment()
	{
		if (!recycledSegments.isEmpty())
		{
			return recycledSegments.pop();
		}
		else
		{
			return new Segment3D();
		}
	}

	/**
	 * Returns a segment to the recycle bin.
	 * 
	 * @param segment the segment that is no longer being used and is safe to be reused later.
	 */
	private static void recycleSegment(Segment3D segment)
	{
		if (segment != null)
		{
			recycledSegments.push(segment);
		}
	}

	/**
	 * Converts vertices into line segments (see helper struct Segment2D)
	 * 
	 * @param objVertices
	 * @param segmentContainer a container that contains all of an objects 2D segments.
	 */
	private static void segmentizeCube(Vector3[] objVertices, ArrayList<Segment3D> segmentContainer)
	{
		//REQUIRES FOLLOWING VERTEX MAPPING:
		//front face: 0, 1, 2, 3
		//rear face: 4, 5, 6, 7
		
		// FRONT FACE
		for (int i = 0; i < objVertices.length / 2; ++i)
		{
			Segment3D segment = getNewSegment();
			segment.firstVert.set(objVertices[i]);
			if (i + 1 < objVertices.length / 2) // could shift for speed
			{
				// use the next vertex as the connecting point
				segment.secondVert.set(objVertices[i + 1]);
			}
			else
			{
				// wrap around and use the first vertex as the starting point
				segment.secondVert.set(objVertices[0]);
			}
			segmentContainer.add(segment);
		}
		// REAR FACE
		for (int i = objVertices.length / 2; i < objVertices.length; ++i)
		{
			Segment3D segment = getNewSegment();
			segment.firstVert.set(objVertices[i]);
			if (i + 1 < objVertices.length) // could shift for speed
			{
				// use the next vertex as the connecting point
				segment.secondVert.set(objVertices[i + 1]);
			}
			else
			{
				// wrap around and use the MIDDLE vertex as the starting point
				segment.secondVert.set(objVertices[objVertices.length / 2]);
			}
			segmentContainer.add(segment);
		}

		// CONNECTIONS BETWEEN REAR AND FRONT
		for (int i = 0; i < objVertices.length / 2; ++i)
		{
			Segment3D segment = getNewSegment();
			segment.firstVert.set(objVertices[i]);
			segment.secondVert.set(objVertices[i + 4]); //see required mapping above, this should connect corners between front and rear faces together.
			segmentContainer.add(segment);
		}
	}

	/**
	 * Converts each segment into its normal vector. A 2D segment is defined by two vectors. The
	 * vector difference between a segments vectors gives a vector that lies on the plane that both
	 * vectors belong to.
	 * 
	 * With a vector that lies on the plane (ie line in 2d) defined by a segment, we can find the
	 * normal vector. The dot product between the vector within the plane and the normal vector
	 * should equal zero (They're perpendicular). We can use linear algebra to solve for the normal
	 * vector.
	 * 
	 * @param segments
	 * @param cubeNormals 
	 */
	public static Vector3 temp1 = new Vector3();
	public static Vector3 temp2 = new Vector3();
	private static void convertSegmentsToAxes(ArrayList<Segment3D> segmentsObj1, ArrayList<Segment3D> segmentsObj2, ArrayList<Vector3> cubeAxes)
	{
		//REQUIRES FOLLOWING VERTEX MAPPING:
		//front face: 0, 1, 2, 3
		//rear face: 4, 5, 6, 7
		//					   0    1    2    3   4    5     6    7   8    9    10    11
		//segments should be {0-1}{1-2}{2-3}{3-1}{4-5}{5-6}{6-7}{7-4}{0-4}{1-5}{2-6}{3-7}
		
		//note: Segmentation isn't necessary for 3d, but it helps conceptually when designing the algorithm
		
		/* Vertex Map
		 *2----1
		 *|    |\ 
		 *3----0 5
		 * \    \|
		 *  7----4
		 */
		
		/* Edge map
		 *  *--1--*
		 *  |     |\
		 *  2     0 9
		 *  |     |  \
		 *  *--3--*   *
		 *   \     \  |
		 *    11    8 5
		 *     \     \|
		 *      *--7--* 
		 */
		
		if(cubeAxes.size() == 0)
			initializeCubeNormalsArray(cubeAxes);
		
		//Generate Normals For Faces (there are duplicate normals, so we can save time by only generating half, there are 3 faces)
		//object 1 face normals
		calculateFaceNorm(cubeAxes.get(0), segmentsObj1.get(0), segmentsObj1.get(1)); //front face; rear face is duplicate
		calculateFaceNorm(cubeAxes.get(1), segmentsObj1.get(0), segmentsObj1.get(9)); //side face; there also exists duplicate
		calculateFaceNorm(cubeAxes.get(2), segmentsObj1.get(1), segmentsObj1.get(9)); //top face; also exists duplicate beneath
		//Object 2 face normals
		calculateFaceNorm(cubeAxes.get(3), segmentsObj2.get(0), segmentsObj2.get(1)); //front face; rear face is duplicate
		calculateFaceNorm(cubeAxes.get(4), segmentsObj2.get(0), segmentsObj2.get(9)); //side face; there also exists duplicate
		calculateFaceNorm(cubeAxes.get(5), segmentsObj2.get(1), segmentsObj2.get(9)); //top face; also exists duplicate beneath
		
		
		//3d requires something a bit more than 2d; we must check if our edges are overlapping, otherwise we may get false positives.
		//Imagine a cubeA setting on a table; imagine cube B leaning onto cubeA so that a edge on the top face of B is setting on a vertical edge of A.
		//We can imaging the two cubes as not quite having collided yet. So, we know the cubes are not colliding (there is some fraction of air between them)
		//However, projecting onto the normal vectors of the faces show overlap for every face.
		//We know the cubes are no colliding, so what axis are we forgetting to project onto?
		//Image that we place a sheet of paper between the two cubes. 
		//Drawing a line out of the normal of this paper will be our axis.
		//By projecting onto this axis, nether cube is having an overlap!
		//To calculate a vector in the plane, we simply need:
		//1)the vectors representing the edge of the cubes, -- we can say these vectors are in the plane of the paper between the cubes
		//2)the resulting vector of the cross product of the two vectors (ie the edges) in the plane; this will be our normal vector and our axis. 
		
		//Since we have parallel edges, there is some redundancy we can remove.
		//Where C represents cube, and E represents edge...
		//We can achieve all axes from: C1E1 X C2E1; C1E1 X C2E2; C1E1 X C2E3; C1E2 X C2E1;C1E2 X C2E2;C1E2 X C2E3;C1E2 X C2E1; C1E3 X C2E2;C1E3 X C2E3
		//edges 0, 1, and 9 our are non-redundant edges
		calculateEdgeAxis(cubeAxes.get(6), segmentsObj1.get(0), segmentsObj2.get(0));
		calculateEdgeAxis(cubeAxes.get(7), segmentsObj1.get(0), segmentsObj2.get(1));
		calculateEdgeAxis(cubeAxes.get(8), segmentsObj1.get(0), segmentsObj2.get(9));
		calculateEdgeAxis(cubeAxes.get(9), segmentsObj1.get(1), segmentsObj2.get(0));
		calculateEdgeAxis(cubeAxes.get(10), segmentsObj1.get(1), segmentsObj2.get(1));
		calculateEdgeAxis(cubeAxes.get(11), segmentsObj1.get(1), segmentsObj2.get(9));
		calculateEdgeAxis(cubeAxes.get(12), segmentsObj1.get(9), segmentsObj2.get(0));
		calculateEdgeAxis(cubeAxes.get(13), segmentsObj1.get(9), segmentsObj2.get(1));
		calculateEdgeAxis(cubeAxes.get(14), segmentsObj1.get(9), segmentsObj2.get(9));
	}
	
	private static void calculateEdgeAxis(Vector3 axisVector, Segment3D obj1Edge, Segment3D obj2Edge)
	{
		//Take differences of vertices in cube to get vector that represents the edge
		temp1.set(obj1Edge.firstVert);
		temp1.sub(obj1Edge.secondVert);
		temp2.set(obj2Edge.firstVert);
		temp2.sub(obj2Edge.secondVert);
		
		//The two edges define a plane between them
		//we want the normal to this plane, it is an axis we must project on. 
		Vector3 edge1 = temp1; //renaming for clarity 
		Vector3 edge2 = temp2;
		axisVector.set(edge1);
		axisVector.crs(edge2);
	}

	private static void calculateFaceNorm(Vector3 faceNormal, Segment3D faceEdge1, Segment3D faceEdge2)
	{
		//take difference of two vectors in a segment to get a vector that is in the plane.
		temp1.set(faceEdge1.firstVert);
		temp1.sub(faceEdge1.secondVert);
		
		//take difference of two vectors in a segment to get a vector that is in the plane.
		temp2.set(faceEdge2.firstVert);
		temp2.sub(faceEdge2.secondVert);
		
		//take the cross product between two vectors in the face plane to the a vector in the normal direction; this is our axis. 
		Vector3 faceVector1 = temp1; //renaming for clarity
		Vector3 faceVector2 = temp2;
		faceNormal.set(faceVector1);
		faceNormal.crs(faceVector2);
	}

	private static void initializeCubeNormalsArray(ArrayList<Vector3> cubeAxes)
	{
		cubeAxes.clear();
		for(int i = 0; i < 15; ++i)
		{
			cubeAxes.add(new Vector3());
		}
		
	}

	/* ------------------------------- HELPER CLASSES ----------------------------- */
	public static class Segment3D
	{
		public float firstVertX = 0;
		public float firstVertY = 0;
		public float secondVertX = 0;
		public float secondVertY = 0;

		public Vector3 firstVert = new Vector3();
		public Vector3 secondVert = new Vector3();
	}
}
