package unit731.boxon.other;

import org.apache.commons.math3.geometry.euclidean.threed.CardanEulerSingularityException;
import org.apache.commons.math3.geometry.euclidean.threed.NotARotationMatrixException;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;


public class RotationHelper{

	private RotationHelper(){}

	/**
	 * Get the Euler angles corresponding to the instance.
	 *
	 * <p>The equations show that each rotation can be defined by two different values of the Euler angles set.<br>
	 * This method implements the following arbitrary choices: the chosen set is the one for which the second angle is between 0 and &pi;
	 * (i.e its sine is positive).</p>
	 *
	 * <p>Euler angle have a very disappointing drawback: all of them have singularities. This means that if the instance is
	 * too close to the singularities corresponding to the given rotation order, it will be impossible to retrieve the angles. For
	 * Cardan angles, this is often called gimbal lock. There is <em>nothing</em> to do to prevent this, it is an intrinsic problem
	 * with Euler representation (but not a problem with the rotation itself, which is perfectly well defined). For Euler angle
	 * singularities occur when the second angle is close to 0 or &pi;, this implies that the identity rotation is always singular
	 * for Euler angles!</p>
	 *
	 * @param m00	The (0, 0) element of the rotation matrix
	 * @param m01	The (0, 1) element of the rotation matrix
	 * @param m02	The (0, 2) element of the rotation matrix
	 * @param m10	The (1, 0) element of the rotation matrix
	 * @param m11	The (1, 1) element of the rotation matrix
	 * @param m12	The (1, 2) element of the rotation matrix
	 * @param m20	The (2, 0) element of the rotation matrix
	 * @param m21	The (2, 1) element of the rotation matrix
	 * @param m22	The (2, 2) element of the rotation matrix
	 * @param threshold	Convergence threshold for the iterative orthogonality correction (convergence is reached when the
	 * 	difference between two steps of the Frobenius norm of the correction is below this threshold)
	 * @return	An array of three angles, in the order specified by the set
	 * @throws NotARotationMatrixException if the matrix is not a 3X3 matrix, or if it cannot be transformed into an orthogonal matrix
	 * 	with the given threshold, or if the determinant of the resulting orthogonal matrix is negative
	 * @throws CardanEulerSingularityException if the rotation is singular with respect to the angles set specified
	 */
	public static double[] getEulerAngles(
			final double m00, final double m01, final double m02,
			final double m10, final double m11, final double m12,
			final double m20, final double m21, final double m22,
			final double threshold) throws NotARotationMatrixException, CardanEulerSingularityException{
		return getEulerAngles(new double[][]{
			new double[]{m00, m01, m02},
			new double[]{m10, m11, m12},
			new double[]{m20, m21, m22}
		}, threshold);
	}

	/**
	 * Get the Euler angles corresponding to the instance.
	 *
	 * <p>The equations show that each rotation can be defined by two different values of the Euler angles set.<br>
	 * This method implements the following arbitrary choices: the chosen set is the one for which the second angle is between 0 and &pi;
	 * (i.e its sine is positive).</p>
	 *
	 * <p>Euler angle have a very disappointing drawback: all of them have singularities. This means that if the instance is
	 * too close to the singularities corresponding to the given rotation order, it will be impossible to retrieve the angles. For
	 * Cardan angles, this is often called gimbal lock. There is <em>nothing</em> to do to prevent this, it is an intrinsic problem
	 * with Euler representation (but not a problem with the rotation itself, which is perfectly well defined). For Euler angle
	 * singularities occur when the second angle is close to 0 or &pi;, this implies that the identity rotation is always singular
	 * for Euler angles!</p>
	 *
	 * @param matrix	Rotation matrix (or nearly rotation matrix)
	 * @param threshold	Convergence threshold for the iterative orthogonality correction (convergence is reached when the
	 * 	difference between two steps of the Frobenius norm of the correction is below this threshold)
	 * @return	An array of three angles, in the order specified by the set
	 * @throws NotARotationMatrixException if the matrix is not a 3X3 matrix, or if it cannot be transformed into an orthogonal matrix
	 * 	with the given threshold, or if the determinant of the resulting orthogonal matrix is negative
	 * @throws CardanEulerSingularityException if the rotation is singular with respect to the angles set specified
	 */
	public static double[] getEulerAngles(final double[][] matrix, final double threshold) throws NotARotationMatrixException,
			CardanEulerSingularityException{
		final Rotation rotation = new Rotation(matrix, threshold);
		return rotation.getAngles(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR);
	}

}
