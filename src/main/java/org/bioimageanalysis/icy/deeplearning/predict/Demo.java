package org.bioimageanalysis.icy.deeplearning.predict;

import java.io.File;
import java.io.FileNotFoundException;

import org.bioimageanalysis.icy.deeplearning.engine.EngineInfo;
import org.bioimageanalysis.icy.deeplearning.model.Model;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Util;

public class Demo
{

	public static < I extends NumericType< I > & NativeType< I >, O extends NumericType< O > & NativeType< O > > void main( final String[] args )
	{
		try
		{
			/*
			 * Load the input image.
			 */
			final String imgPath = "/Users/tinevez/Desktop/DemoModelRunner/sample_input_0.tif";
			final ImagePlus imp = IJ.openImage( imgPath );
			final Img< I > img = ImagePlusAdapter.wrap( imp );
			// Could be determined from the input file.
			final String inputAxes = "xyz";
			System.out.println( "Image loaded: " + img + " with axes: " + inputAxes + " and type: " + img.firstElement().getClass().getSimpleName() );

			/*
			 * Load the model.
			 */
			final Model model = loadModel();
			System.out.println( "Model loaded: " + model ); // DEBUG

			/*
			 * Load the specs.
			 */
			final ModelSpec spec = ModelSpec.from( model );
			System.out.println( "Model specs loaded. Output axes: " + spec.outputAxes + " - Output data type: " + spec.outputType().getClass().getSimpleName() );

			/*
			 * Reshape the input to match the specs.
			 */
			final RandomAccessibleInterval< I > input = AxesMatcher.matchAxes( spec.outputAxes, inputAxes, img );
			System.out.println( "Input reshaped: " + input );

			/*
			 * Prepare holder for results.
			 */
			final ShapeMath shapeMath = new ShapeMath( spec );
			final Interval outputInterval = shapeMath.getOutputInterval( input );
			@SuppressWarnings( "unchecked" )
			final O outputType = ( O ) spec.outputType();
			final ImgFactory< O > factory = Util.getArrayOrCellImgFactory( outputInterval, outputType );
			final Img< O > output = factory.create( outputInterval );
			System.out.println( "Output holder prepared: " + output );

			/*
			 * Run the model on the input writing in the output image.
			 */

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

