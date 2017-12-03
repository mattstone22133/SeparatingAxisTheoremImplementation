package sat.simulation;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class SATMain extends ApplicationAdapter
{
	private Application2DPresentation presentation;
	private Application2D application2D;
	private Application3D application3D;
	private ApplicationAdapter current;

	@Override
	public void create()
	{
		application2D = new Application2D();
		application2D.create();
		
		application3D = new Application3D();
		application3D.create();
		
		presentation = new Application2DPresentation();
		presentation.create();
		
		current = presentation;
	}

	@Override
	public void render()
	{
		current.render();
		
		if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1))
		{
			//current = application2D;
			current = presentation;
		}
		if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_2))
		{
			current = application3D;
		}
	}

	@Override
	public void dispose()
	{
		application2D.dispose();
		application3D.dispose();
		presentation.dispose();
	}
}
