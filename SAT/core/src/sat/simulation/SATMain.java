package sat.simulation;

import com.badlogic.gdx.ApplicationAdapter;

public class SATMain extends ApplicationAdapter
{
	private Application2D application2D;

	@Override
	public void create()
	{
		application2D = new Application2D();
		application2D.create();
	}

	@Override
	public void render()
	{
		application2D.render();
	}

	@Override
	public void dispose()
	{
		application2D.dispose();
	}
}
