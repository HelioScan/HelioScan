/*
 * Mixture Modeling algorithm
 *
 * Copyright (c) 2003 by Christopher Mei (christopher.mei@sophia.inria.fr)
 *                    and Maxime Dauphin
 *
 * This plugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this plugin; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

import java.util.*;
import ij.*;
import ij.process.*;

/** This class implements a GrayLevelClassMixtureModeling.
 **/

public class GrayLevelClassMixtureModeling {
  public static int[] histogram;
    /** The index must vary between 1 and 253
	C1 : [0;index]
	C2 : [index+1; 255]
    **/
    private int index;
    private float mu1, mu2;
    private float sigma2_1, sigma2_2;
    private float mult1, mult2;
    private float twoVariance1, twoVariance2;
    private float max1, max2;
    private int cardinal1, cardinal2;
    private int cardinal;

    private int INDEX_MIN = 1;
    private int INDEX_MAX = 253;
    private int MIN = 0;
    public static final int MAX = 255;
    
    public GrayLevelClassMixtureModeling(ByteProcessor img) {
	cardinal = img.getWidth()*img.getHeight();
	histogram = img.getHistogram();
	index = INDEX_MIN-1;
	//setValues();
    }

    public boolean addToIndex() {
	index++;

	if(!(index<=INDEX_MAX))
	    return false;	

	setValues();
	return true;
    }

    private float calculateMax(int index) {
	float sum = histogram[index];
	float num = 1;
	if(index-1>=0) {
	    sum += histogram[index-1];
	    num++;
	}
	if(index+1<MAX) {
	    sum += histogram[index+1];
	    num++;
	}
	return sum/num;
    }

    public String toString() {
	StringBuffer ret = new StringBuffer();
	
	ret.append("Index : "+index+"\n");
	ret.append("Max1 : "+max1+" ");
	ret.append("Max2 : "+max2+"\n");
	ret.append("Mu1 : "+mu1+" ");
	ret.append("Mu2 : "+mu2+"\n");
	ret.append("Cardinal1 : "+cardinal1+" ");
	ret.append("Cardinal2 : "+cardinal2+"\n");
	ret.append("Variance1 : "+sigma2_1+" ");
	ret.append("Variance2 : "+sigma2_2+"\n");
	
	return ret.toString();
    }

    public float getCardinal() {
	return cardinal;
    }

    public float getMu1() {
	return mu1;
    }

    public float getMu2() {
	return mu2;
    }

    public float getMax1() {
	return max1;
    }

    public float getMax2() {
	return max2;
    }

    public float getVariance1() {
	return sigma2_1;
    }

    public float getVariance2() {
	return sigma2_2;
    }

    public float getCardinal1() {
	return cardinal1;
    }

    public float getCardinal2() {
	return cardinal2;
    }

    public int getThreshold() {
	return index;
    }

    public void setIndex(int index) {
	this.index = index;
	setValues();
    }

    private void setValues() {	
	mu1 = 0; mu2 = 0;
	sigma2_1 = 0; sigma2_2 = 0;
	max1 = 0; max2 = 0;
	cardinal1 = 0; cardinal2 = 0;

	for(int i=MIN; i<=index ; i++) {
	    cardinal1 +=  histogram[i];
	    mu1 +=  i*histogram[i];
	}
    
	for(int i=index+1; i<=MAX ; i++) {
	    cardinal2 +=  histogram[i];
	    mu2 +=  i*histogram[i];
	}
    
	if(cardinal1 == 0) {
	    mu1 = 0;
	    sigma2_1 = 0;

	}
	else 
	    mu1 /= (float)cardinal1; 

	if(cardinal2 == 0) {
	    mu2 = 0;
	    sigma2_2 = 0;
	}
	else 
	    mu2 /= (float)cardinal2; 

	if( mu1 != 0 ) {
	    for(int i=MIN; i<=index ; i++) 
		sigma2_1 += histogram[i]*Math.pow(i-mu1,2);
	    
	    sigma2_1 /= (float)cardinal1;
	
	    max1 = calculateMax((int) mu1);

	    mult1 = (float) max1;
	    twoVariance1 = 2*sigma2_1; 
	}
	if( mu2 != 0 ) {
	    for(int i=index+1; i<=MAX ; i++) 
		sigma2_2 += histogram[i]*Math.pow(i-mu2,2);
	    
	    sigma2_2 /= (float)cardinal2;
	
	    max2 = calculateMax((int) mu2);

	    mult2 = (float) max2;
	    twoVariance2 = 2*sigma2_2; 
	}
    }

    public final float gamma1(int i) {
	if(sigma2_1 == 0) 
	    return 0;
	return (float)(mult1*Math.exp(-(Math.pow((float)i-mu1,2))/twoVariance1));
    }

    public final float gamma2(int i) {
	if(sigma2_2 == 0) 
	    return 0;
	return (float)(mult2*Math.exp(-(Math.pow((float)i-mu2,2))/twoVariance2));
    }

    public float gamma(int i) {
	return gamma1(i)+gamma2(i);
    }

    public float differenceGamma(int i) {
	return gamma1(i)-gamma2(i);
    }

    public static int getHistogram(int i) {
	return histogram[i];
    }
}
