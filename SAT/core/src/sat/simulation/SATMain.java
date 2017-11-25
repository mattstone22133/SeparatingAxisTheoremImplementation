package sat.simulation;

import com.badlogic.gdx.ApplicationAdapter;

public class SATMain extends ApplicationAdapter
{
	private Application2D application2D;
	private Application3D application3D;

	@Override
	public void create()
	{
		application2D = new Application2D();
		application2D.create();
		
		application3D = new Application3D();
		application3D.create();
	}

	@Override
	public void render()
	{
		//application2D.render();
		application3D.render();
	}

	@Override
	public void dispose()
	{
		application2D.dispose();
		application3D.dispose();
	}
}
