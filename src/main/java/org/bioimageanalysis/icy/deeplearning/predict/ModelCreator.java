package org.bioimageanalysis.icy.deeplearning.predict;

import java.io.File;

import io.bioimage.modelrunner.engine.EngineInfo;
import io.bioimage.modelrunner.model.Model;

public class ModelCreator
{
	public static Model fromFiles( final String modelDirectory, final String weightsFileName, final String engineDirectory, final boolean cpu, final boolean gpu, final String engine, final String engineVersion ) throws Exception
	{
		final EngineInfo engineInfo =
				EngineInfo.defineDLEngine(
						engine,
						engineVersion,
						engineDirectory,
						cpu,
						gpu );

		final Model model =
				Model.createDeepLearningModel(
						modelDirectory,
						new File( modelDirectory, weightsFileName ).getAbsolutePath(),
						engineInfo );

		model.loadModel();

		return model;
	}
}
