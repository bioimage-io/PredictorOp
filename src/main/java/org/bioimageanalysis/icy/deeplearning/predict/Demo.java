package org.bioimageanalysis.icy.deeplearning.predict;

import java.io.File;
import java.io.FileNotFoundException;

import org.bioimageanalysis.icy.deeplearning.engine.EngineInfo;
import org.bioimageanalysis.icy.deeplearning.model.Model;

public class Demo
{

	public static void main( final String[] args )
	{
		final Model model = loadModel();
		try
		{
			final ModelSpec spec = ModelSpec.from( model );
			System.out.println( spec ); // DEBUG
		}
		catch ( final FileNotFoundException e )
		{
			e.printStackTrace();
		}


	}

	private static Model loadModel()
	{
		try
		{
			final String rootFolder = "/Users/tinevez/Desktop/DemoModelRunner";
			final String engine = "torchscript";
			final String engineVersion = "1.9.1";
			final String enginesDir = "/Users/tinevez/Development/Mastodon/model-runner-java/engines";
			final String modelFolder = new File( rootFolder, "platynereisemnucleisegmentationboundarymodel_torchscript" ).getAbsolutePath();
			final String modelSource = new File( modelFolder, "/weights-torchscript.pt" ).getAbsolutePath();
			final boolean cpu = true;
			final boolean gpu = false;
			final EngineInfo engineInfo = EngineInfo.defineDLEngine( engine, engineVersion, enginesDir, cpu, gpu );
			final Model model = Model.createDeepLearningModel( modelFolder, modelSource, engineInfo );
			model.loadModel();
			return model;
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}
}

