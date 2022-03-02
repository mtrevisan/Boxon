/*
 * Copyright (c) 2020-2022 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.boxon.core;

import java.util.concurrent.TimeUnit;


public final class TimeWatch{

	private static final String TIMER_NOT_STOPPED = "timer not stopped";
	private enum Scale{
		SECONDS("s"),
		MILLIS("ms"),
		MICROS("µs");

		private final String uom;

		Scale(final String uom){
			this.uom = uom;
		}
	}


	private long start;
	private long end;


	private TimeWatch(){
		reset();
	}


	/**
	 * Start the timer.
	 *
	 * @return	The instance.
	 */
	public static TimeWatch start(){
		return new TimeWatch();
	}

	/**
	 * Restart the timer.
	 *
	 * @return	This instance, used for chaining.
	 */
	public TimeWatch reset(){
		start = System.nanoTime();

		return this;
	}

	/**
	 * Stops the current timer.
	 *
	 * @return	This instance, used for chaining.
	 */
	public TimeWatch stop(){
		end = System.nanoTime();

		return this;
	}

	/**
	 * The elapsed time.
	 *
	 * @return	The elapsed time [ns], or a negative number if not stopped.
	 */
	public long time(){
		return (end > 0l? end - start: -1l);
	}

	/**
	 * The elapsed time.
	 *
	 * @param uom	The unit of measure.
	 * @return	The elapsed time.
	 */
	public long time(final TimeUnit uom){
		return uom.convert(time(), TimeUnit.NANOSECONDS);
	}

	/**
	 * A string representation of the (mean) elapsed time considering the given runs.
	 *
	 * @param runs	The number of runs the elapsed time will be divided to.
	 * @return	The string representation of the mean elapsed time.
	 */
	public String toString(final int runs){
		if(end < 0l)
			return TIMER_NOT_STOPPED;

		double delta = (end - start) / (runs * 1_000.);
		final Scale[] scaleValues = Scale.values();
		int scale = scaleValues.length - 1;
		while(scale >= 0 && delta >= 1_500.){
			delta /= 1000.;
			scale --;
		}
		final int fractionalDigits = (delta > 100? 0: (delta > 10? 1: 2));

		final StringBuilder sb = new StringBuilder(16);
		format(sb, delta, fractionalDigits);
		sb.append(' ').append(scaleValues[scale].uom);
		return sb.toString();
	}

	private static StringBuilder format(final StringBuilder sb, final double value, final int fractionalDigits){
		long factor = (long)StrictMath.pow(10., fractionalDigits);
		final long scaled = (long)(value * factor + 0.5);
		int scale = fractionalDigits;
		final long scaled2 = scaled / 10;
		while(factor <= scaled2){
			factor *= 10;
			scale ++;
		}
		while(scale >= 0){
			if(scale == fractionalDigits - 1)
				sb.append('.');

			final long c = (scaled / factor) % 10;
			scale --;
			factor /= 10;
			sb.append((char)('0' + c));
		}
		return sb;
	}

}
