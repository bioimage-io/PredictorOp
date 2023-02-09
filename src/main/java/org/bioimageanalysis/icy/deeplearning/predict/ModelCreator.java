package org.bioimageanalysis.icy.deeplearning.predict;

import org.bioimageanalysis.icy.deeplearning.engine.EngineInfo;
import org.bioimageanalysis.icy.deeplearning.model.Model;

import java.io.File;

public class ModelCreator
{
	public static Model fromFiles( String modelDirectory, String weightsFileName, String engineDirectory, boolean cpu, boolean gpu, String engine, String engineVersion ) throws Exception
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
