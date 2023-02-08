package org.bioimageanalysis.icy.deeplearning.predict;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

public class AxesMatcher
{

	public static abstract class DimensionalityModification
	{

		protected final String dim;

		public DimensionalityModification( final String dim )
		{
			this.dim = dim;
		}

		public abstract String updateString( final String axes );

		public abstract < T > RandomAccessibleInterval< T > updateRAI( RandomAccessibleInterval< T > rai );

		public static DimensionAdded add( final int position, final String dim )
		{
			return new DimensionAdded( position, dim );
		}

		public static DimensionDropped remove( final int position, final String dim )
		{
			return new DimensionDropped( position, dim );
		}

		public static DimensionFlip flip( final int from, final int to, final String dims )
		{
			return new DimensionFlip( from, to, dims );
		}

	}

	public static class DimensionAdded extends DimensionalityModification
	{

		/**
		 * Position in the desired dimension array at which to add the
		 * dimension.
		 */
		private final int position;

		private DimensionAdded( final int position, final String dim )
		{
			super( dim );
			this.position = position;
		}

		@Override
		public String updateString( final String axes )
		{
			return axes.substring( 0, position ) + dim + axes.substring( position );
		}

		@Override
		public < T > RandomAccessibleInterval< T > updateRAI( final RandomAccessibleInterval< T > inputRAI )
		{
			RandomAccessibleInterval< T > out = Views.addDimension( inputRAI, 0, 0 );
			out = Views.moveAxis( out, out.numDimensions() - 1, position );
			return out;
		}

		@Override
		public String toString()
		{
			return "add " + dim + " at " + position;
		}
	}

	private static class DimensionDropped extends DimensionalityModification
	{

		/**
		 * Position in the original dimension array where the dimension is to be
		 * removed.
		 */
		private final int position;

		private DimensionDropped( final int position, final String dim )
		{
			super( dim );
			this.position = position;
		}

		@Override
		public String updateString( final String axes )
		{
			final int i1 = Math.max( 0, position - 1 );
			final int i2 = Math.min( position, axes.length() - 1 );
			return axes.substring( 0, i1 ) + axes.substring( i2 );
		}

		@Override
		public < T > RandomAccessibleInterval< T > updateRAI( final RandomAccessibleInterval< T > desiredRAI )
		{
			throw new IllegalStateException( "The DimensionDropped change is not supposed to be used on the input RAI." );
		}

		@Override
		public String toString()
		{
			return "drop " + dim + " at " + position;
		}


	}

	private static class DimensionFlip extends DimensionalityModification
	{

		/**
		 * Position in the original dimension array to flip from.
		 */
		private final int from;

		/**
		 * Position in the original dimension array to flip to.
		 */
		private final int to;

		private DimensionFlip( final int from, final int to, final String dims )
		{
			super( dims );
			this.from = from;
			this.to = to;
		}

		@Override
		public < T > RandomAccessibleInterval< T > updateRAI( final RandomAccessibleInterval< T > rai )
		{
			return Views.moveAxis( rai, from, to );
		}

		@Override
		public String updateString( final String axes )
		{
			final char[] chars = axes.toCharArray();
			final char c1 = chars[ from ];
			chars[ from ] = chars[ to ];
			chars[ to ] = c1;
			return String.copyValueOf( chars );
		}

		@Override
		public String toString()
		{
			return "flip " + dim.charAt( 0 ) + " and " + dim.charAt( 1 ) + " between " + from + " and " + to;
		}

	}

	private static final String DIM_NAMES = "tbczyx";

	/**
	 * Adds dimensions and flip dimensions to a view of the specified RAI with
	 * the specified input axes string, so that its dimensions matches the
	 * desired axes string specified.
	 * 
	 * @param <T>
	 *            the type of the pixel in the input RAI.
	 * @param desiredAxes
	 *            the desired axes string. Something like "bczyx".
	 * @param inputAxes
	 *            the axes string of the input RAI. Something like "xyz".
	 * @param input
	 *            the input RAI.
	 * @return a view of the input RAI, with a number of dimensions equal to the
	 *         length of the desired axes string, with dimension properly
	 *         flipped.
	 * @throws IllegalArgumentException
	 *             if: 1/ the number of dimensions in the input RAI is not equal
	 *             to the length of the input axes string. 2/ If one of the two
	 *             axes string is not made of the chars "btczyx".
	 */
	public static final < T > RandomAccessibleInterval< T > matchAxes( String desiredAxes, String inputAxes, final RandomAccessibleInterval< T > input )
	{
		/*
		 * Various checks.
		 */
		if (input.numDimensions() != inputAxes.length())
			throw new IllegalArgumentException( "The input image has " + input.numDimensions() + " dimensions but the input axes specifications has " + inputAxes.length() + " characters." );
		checkAuthorizedChars( desiredAxes );
		checkAuthorizedChars( inputAxes );

		final List< DimensionalityModification > sequence = new ArrayList<>();
		
		/*
		 * Check dimensions to drop.
		 */
		for ( int i = 0; i < inputAxes.length(); i++ )
		{
			final char c = inputAxes.charAt( i );
			if ( desiredAxes.indexOf( c ) < 0 )
			{
				final DimensionDropped change = DimensionalityModification.remove( i, "" + c );
				desiredAxes = change.updateString( desiredAxes );
				sequence.add( change );
			}
		}

		/*
		 * Flip axes to match imglib2 order.
		 */
//		inputAxes = new StringBuilder( inputAxes ).reverse().toString();
//		desiredAxes = new StringBuilder( desiredAxes ).reverse().toString();

		/*
		 * Check missing dimensions from the input.
		 */
		for ( int i = 0; i < desiredAxes.length(); i++ )
		{
			final char c = desiredAxes.charAt( i );
			if ( inputAxes.indexOf( c ) < 0 )
			{
				final DimensionAdded change = DimensionalityModification.add( i, "" + c );
				inputAxes = change.updateString( inputAxes );
				sequence.add( change );
			}
		}

		/*
		 * Check dimensions to flip.
		 */
		for ( int i = 0; i < inputAxes.length(); i++ )
		{
			final char c = inputAxes.charAt( i );
			final int j = desiredAxes.indexOf( c );
			if ( j < 0 || i == j )
				continue;
			final char d = desiredAxes.charAt( i );
			final DimensionFlip change = DimensionalityModification.flip( i, j, "" + c + d );
			inputAxes = change.updateString( inputAxes );
			sequence.add( change );
		}

		/*
		 * Execute sequence.
		 */
		RandomAccessibleInterval< T > output = input;
		for ( final DimensionalityModification change : sequence )
		{
			if ( change instanceof DimensionDropped )
				continue; // Concerns the desired RAI, which we do not touch.
			output = change.updateRAI( output );
		}
		return output;
	}

	private static void checkAuthorizedChars( final String axes )
	{
		for ( final char c : axes.toCharArray() )
			if ( DIM_NAMES.indexOf( c ) < 0 )
				throw new IllegalArgumentException( "Axes string can only contain " + DIM_NAMES + ", but char " + c + " was found" );
	}

	public static void main( final String[] args )
	{
		final String inputAxes = "xyz";
		final String desiredAxes = "bczyxt";
		final RandomAccessibleInterval< UnsignedByteType > img = ArrayImgs.unsignedBytes( 100, 80, 20 );
		System.out.println( img );
		final RandomAccessibleInterval< UnsignedByteType > out = matchAxes( desiredAxes, inputAxes, img );
		System.out.println( out );
	}
}
