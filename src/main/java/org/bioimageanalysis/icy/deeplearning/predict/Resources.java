package org.bioimageanalysis.icy.deeplearning.predict;

public class Resources
{
	// J-Y Platyneris 3D
//	public static final String MODEL_FOLDER = "/Users/tinevez/Desktop/DemoModelRunner/platynereisemnucleisegmentationboundarymodel_torchscript";
//	public static final String EXAMPLE_IMAGE = "/Users/tinevez/Desktop/DemoModelRunner/sample_input_0.tif";
//	public static final String ENGINES_FOLDER = "/Users/tinevez/Development/Mastodon/model-runner-java/engines";
//	public static final boolean USE_CPU = true;
//	public static final boolean USE_GPU = false;

	// J-Y cells in PC 2D - does not work on Mac. Tensorflow problem?
	public static final String MODEL_FOLDER = "/Users/tinevez/Desktop/DemoModelRunner/pancreatic-phase-contrast-cell-segmentation-(u-net)_tensorflow_saved_model_bundle/";
	public static final String EXAMPLE_IMAGE = MODEL_FOLDER + "exampleImage.tiff";
	public static final String ENGINES_FOLDER = "/Users/tinevez/Development/Mastodon/model-runner-java/engines";
	public static final boolean USE_CPU = true;
	public static final boolean USE_GPU = false;

	// J-Y bacteria 2D
//	public static final String MODEL_FOLDER = "/Users/tinevez/Desktop/DemoModelRunner/-b.-sutilist-bacteria-segmentation---widefield-microscopy---2d-unet_keras_hdf5/";
//	public static final String EXAMPLE_IMAGE = MODEL_FOLDER + "sample_input_0.tif";
//	public static final String ENGINES_FOLDER = "/Users/tinevez/Development/Mastodon/model-runner-java/engines";
//	public static final boolean USE_CPU = true;
//	public static final boolean USE_GPU = false;

	// TISCHI
//	public static final String MODEL_FOLDER = "/Users/tischer/Desktop/deep-models/platynereisemnucleisegmentationboundarymodel_torchscript";
//	public static final String ENGINES_FOLDER = "/Users/tischer/Desktop/deep-engines";
//	public static final String EXAMPLE_IMAGE = MODEL_FOLDER + "/" + "sample_input_0.tif";
//	public static final boolean USE_CPU = true;
//	public static final boolean USE_GPU = true;

	// TOBY
}
