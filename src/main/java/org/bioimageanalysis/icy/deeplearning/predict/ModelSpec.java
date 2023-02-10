package org.bioimageanalysis.icy.deeplearning.predict;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bioimageanalysis.icy.deeplearning.model.Model;
import org.yaml.snakeyaml.Yaml;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;

public class ModelSpec
{

	private static final String MODEL_SPEC_FILENAME = "rdf.yaml";

	public final String inputAxes;

	public final String inputDataType;

	public final int[] inputShapeMin;

	public final int[] inputShapeStep;

	public final String outputAxes;

	public final String outputDataType;

	public final int[] outputHalo;

	public final double[] outputShapeOffset;

	public final double[] outputShapeScale;

	public final double[] outputDataRange;

	public final WeightType weightType;

	public final String weightSource;

	public final String weightVersion;

	public ModelSpec(
			final String inputAxes,
			final String inputDataTypeStr,
			final int[] inputShapeMin,
			final int[] inputShapeStep,
			final String outputAxes,
			final String outputDataTypeStr,
			final int[] outputHalo,
			final double[] outputShapeOffset,
			final double[] outputShapeScale,
			final double[] outputDataRange,
			final WeightType weightType,
			final String weightSource,
			final String weightVersion )
	{
		this.inputAxes = inputAxes;
		this.inputDataType = inputDataTypeStr;
		this.inputShapeMin = inputShapeMin;
		this.inputShapeStep = inputShapeStep;
		this.outputAxes = outputAxes;
		this.outputDataType = outputDataTypeStr;
		this.outputHalo = outputHalo;
		this.outputShapeOffset = outputShapeOffset;
		this.outputShapeScale = outputShapeScale;
		this.outputDataRange = outputDataRange;
		this.weightType = weightType;
		this.weightSource = weightSource;
		this.weightVersion = weightVersion;
	}

	/**
	 * Returns an ImgLib2 type object matching the output pixel type.
	 * 
	 * @param <T>
	 *            the type.
	 * @return the type.
	 */
	@SuppressWarnings( "unchecked" )
	public < T extends RealType< T > & NativeType< T > > T outputType()
	{
		switch ( outputDataType.trim().toLowerCase() )
		{
		case "uint8":
			return ( T ) new UnsignedByteType();
		case "int8":
			return ( T ) new ByteType();
		case "uint16":
			return ( T ) new UnsignedShortType();
		case "int16":
			return ( T ) new ShortType();
		case "float32":
			return ( T ) new FloatType();
		case "float64":
			return ( T ) new DoubleType();
		default:
			throw new IllegalArgumentException( "Unknown output data type: " + outputDataType );
		}
	}

	@Override
	public String toString()
	{
		final StringBuilder result = new StringBuilder();
		final String newLine = System.getProperty( "line.separator" );

		result.append( this.getClass().getName() );
		result.append( " Object {" );
		result.append( newLine );

		final Field[] fields = this.getClass().getDeclaredFields();
		for ( final Field field : fields )
		{
			result.append( " - " );
			try
			{
				result.append( field.getName() );
				result.append( ": " );
				final Object val = field.get( this );
				final String valStr;
				if ( val instanceof int[] )
					valStr = Util.printCoordinates( ( int[] ) val );
				else if ( val instanceof double[] )
					valStr = Util.printCoordinates( ( double[] ) val );
				else
					valStr = val.toString();
				result.append( "\t" + valStr );
			}
			catch ( final IllegalAccessException ex )
			{
				System.out.println( ex );
			}
			result.append( newLine );
		}
		result.append( "}" );
		return result.toString();
	}

	private static final int[] getIntArrayFromMap( final Map< String, Object > map, final String key )
	{
		@SuppressWarnings( "unchecked" )
		final List< Number > l = ( List< Number > ) map.get( key );
		final int[] out = new int[ l.size() ];
		for ( int i = 0; i < out.length; i++ )
			out[ i ] = l.get( i ).intValue();
		return out;
	}

	private static double[] getDoubleArrayFromMap( final Map< String, Object > map, final String key )
	{
		@SuppressWarnings( "unchecked" )
		final List< Object > l = ( List< Object > ) map.get( key );
		final double[] out = new double[ l.size() ];
		for ( int i = 0; i < out.length; i++ )
		{
			final Object obj = l.get( i );
			if ( obj instanceof String )
			{
				final String str = ( String ) obj;
				if ( str.trim().toLowerCase().equals( "-inf" ) )
					out[ i ] = Double.NEGATIVE_INFINITY;
				else if ( str.trim().toLowerCase().equals( "inf" ) )
					out[ i ] = Double.POSITIVE_INFINITY;
				else
					out[ i ] = Double.valueOf( ( String ) obj );
			}
			else
				out[ i ] = ( ( Number ) obj ).doubleValue();
		}
		return out;
	}

	private static Map< String, Object > getInnerMap( final Map< String, Object > map, final String key )
	{
		final Object obj = map.get( key );
		if ( obj instanceof List )
		{
			@SuppressWarnings( "rawtypes" )
			final List list = ( List ) map.get( key );
			@SuppressWarnings( "unchecked" )
			final Map< String, Object > out = ( Map< String, Object > ) list.get( 0 );
			return out;
		}
		else
		{
			@SuppressWarnings( "unchecked" )
			final Map< String, Object > out = ( Map< String, Object > ) obj;
			return out;
		}
	}

	public static ModelSpec fromModel( final Model model ) throws FileNotFoundException
	{
		final String modelFolder = model.getModelFolder();
		final File rdfFile = new File( modelFolder, MODEL_SPEC_FILENAME );
		return fromFile( rdfFile.getAbsolutePath() );
	}

	public static ModelSpec fromFolder( final String modelFolder ) throws FileNotFoundException
	{
		final File rdfFile = new File( modelFolder, "rdf.yaml" );
		return fromFile( rdfFile.getAbsolutePath() );
	}

	@SuppressWarnings( "rawtypes" )
	private static final int[][] getShapeArrays( final Map< String, Object > inputsMap )
	{
		final Object obj = inputsMap.get( "shape" );
		if ( obj instanceof List && ( ( List ) obj ).get( 0 ) instanceof Integer )
		{
			@SuppressWarnings( "unchecked" )
			final List< Integer > explicitShape = ( List< Integer > ) obj;
			final int[] min = new int[ explicitShape.size() ];
			final int[] step = new int[ explicitShape.size() ];
			for ( int d = 0; d < step.length; d++ )
			{
				min[ d ] = explicitShape.get( d );
				step[ d ] = 0;
			}
			return new int[][] { min, step };
		}
		else
		{
			final Map< String, Object > inputsShapeMap = getInnerMap( inputsMap, "shape" );
			final int[] min = getIntArrayFromMap( inputsShapeMap, "min" );
			final int[] step = getIntArrayFromMap( inputsShapeMap, "step" );
			return new int[][] { min, step };
		}
	}

	public static ModelSpec fromFile( final String rdfPath ) throws FileNotFoundException
	{
		final Yaml yaml = new Yaml();
		final FileReader input = new FileReader( rdfPath );
		final Iterable< Object > objs = yaml.loadAll( input );
		// Let's not be subtle.
		@SuppressWarnings( "unchecked" )
		final Map< String, Object > map = ( Map< String, Object > ) objs.iterator().next();

		// Inputs.
		@SuppressWarnings( "unchecked" )
		final Map< String, Object > inputsMap = ( Map< String, Object > ) ( ( List< Object > ) map.get( "inputs" ) ).get( 0 );
		final String inputAxes = ( String ) inputsMap.get( "axes" );
		final String inputDataTypeStr = ( String ) inputsMap.get( "data_type" );

		final int[][] shapeArrays = getShapeArrays( inputsMap );
		final int[] inputShapeMin = shapeArrays[ 0 ];
		final int[] inputShapeStep = shapeArrays[ 1 ];

		// Outputs.
		@SuppressWarnings( "unchecked" )
		final Map< String, Object > outputsMap = ( Map< String, Object > ) ( ( List< Object > ) map.get( "outputs" ) ).get( 0 );
		final String outputAxes = ( String ) outputsMap.get( "axes" );
		final String outputDataTypeStr = ( String ) outputsMap.get( "data_type" );
		final double[] outputDataRange = getDoubleArrayFromMap( outputsMap, "data_range" );
		final int[] outputHalo = getHalo( outputsMap, outputAxes.length() );
		@SuppressWarnings( "unchecked" )
		final Map< String, Object > outputsShapeMap = ( Map< String, Object > ) outputsMap.get( "shape" );
		final double[] outputShapeOffset = getDoubleArrayFromMap( outputsShapeMap, "offset" );
		final double[] outputShapeScale = getDoubleArrayFromMap( outputsShapeMap, "scale" );

		// Weights.
		final Map< String, Object > weightsMap = getInnerMap( map, "weights" );
		final Entry< String, Object > weightEntry = weightsMap.entrySet().iterator().next();
		final String weightFormat = weightEntry.getKey();
		final WeightType wt = WeightType.fromFormat( weightFormat );
		@SuppressWarnings( "unchecked" )
		final Map< String, Object > weightSpecs = ( Map< String, Object > ) weightEntry.getValue();
		final String weightSource = ( String ) weightSpecs.get( "source" );
		final String weightVersion = wt.tryToReadVersion( weightSpecs );

		return new ModelSpec(
				inputAxes,
				inputDataTypeStr,
				inputShapeMin,
				inputShapeStep,
				outputAxes,
				outputDataTypeStr,
				outputHalo,
				outputShapeOffset,
				outputShapeScale,
				outputDataRange,
				wt,
				weightSource,
				weightVersion );
	}


	private static int[] getHalo( final Map< String, Object > outputsMap, final int nDims )
	{
		if ( outputsMap.containsKey( "halo" ) )
			return getIntArrayFromMap( outputsMap, "halo" );

		final int[] defaultHalo = new int[ nDims ];
		Arrays.fill( defaultHalo, 0 );
		return defaultHalo;
	}

	public enum WeightType
	{
		PYTORCH( "pytorch_state_dict" ),
		TORCHSCRIPT( "torchscript" ),
		KERAS( "keras_hdf5" ),
		TENSORTFLOW_JS( "tensorflow_js" ),
		TENSORTFLOW_BUNDLE( "tensorflow_saved_model_bundle" ),
		ONNX( "onnx" );

		private final String format;

		WeightType( final String format )
		{
			this.format = format;
		}

		String tryToReadVersion( final Map< String, Object > weightSpecs )
		{
			// This field is optional so....
			switch ( this )
			{
			case ONNX:
				return ( String ) weightSpecs.getOrDefault( "opset_version", "" );
			case PYTORCH:
			case TORCHSCRIPT:
				return ( String ) weightSpecs.getOrDefault( "pytorch_version", "" );
			case KERAS:
			case TENSORTFLOW_BUNDLE:
			case TENSORTFLOW_JS:
				return ( String ) weightSpecs.getOrDefault( "tensorflow_version", "" );
			default:
				return "";
			}
		}

		public String getFormat()
		{
			return format;
		}

		public static WeightType fromFormat(final String format)
		{
			final String str = format.trim().toLowerCase();
			for ( final WeightType wt : WeightType.values() )
			{
				if (str.equals( wt.format.trim().toLowerCase() ))
					return wt;
			}
			throw new IllegalArgumentException( "Unknown weight type format: " + format );
		}
	}

	public static void main( final String[] args ) throws FileNotFoundException
	{
		final String rdfPath = Resources.MODEL_FOLDER;
		final ModelSpec info = fromFolder( rdfPath );
		System.out.println( info );
	}
}
