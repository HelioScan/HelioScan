/*
 * Anisotropic diffusion plugin
 *
 * Copyright (c) 2003 by Christopher Mei (christopher.mei@sophia.inria.fr)
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

import java.lang.*;
import java.util.*;
import ij.*;

/**
 *  The aim of AnisotropicDiffusionPixel is to create
 *  a structure to easily work on neighbouring pixels.
 *  
 **/

public class AnisotropicDiffusionPixel {
    /** x coordinate of the pixel **/
    private int x;
    /** y coordinate of the pixel **/
    private int y;

    /** grayscale values of the pixel **/
    private byte currentHeight; 
    private byte newHeight;

    /** Neighbours **/
    /**N, S, E, W neighbours **/
    private Vector directNeighbours;
    /**NE, SE, SW, NW neighbours **/
    private Vector diagNeighbours;

    public static double K;
    public static double lambda;

    public AnisotropicDiffusionPixel(int x, int y, byte height) {
	this.x = x;
	this.y = y;
	this.currentHeight = height;

	directNeighbours = new Vector(4);
	diagNeighbours = new Vector(4);
	newHeight = 0;
    }

    public void addDirectNeighbour(AnisotropicDiffusionPixel neighbour) {
	/*IJ.write("In Pixel, adding :");
	  IJ.write(""+neighbour);
	  IJ.write("Add done");
	*/
	directNeighbours.add(new Neighbour(neighbour,0));
    }

    public void addDiagNeighbour(AnisotropicDiffusionPixel neighbour) {
	/*IJ.write("In Pixel, adding :");
	  IJ.write(""+neighbour);
	  IJ.write("Add done");
	*/
	diagNeighbours.add(new Neighbour(neighbour,0));
    }

    /** Calculation of the new value of the pixel
     **/

    public void setNewHeight(boolean bigRegionFunction) {
	double sum = 0;
	
	for(int i=0 ; i<directNeighbours.size() ; i++) {
	    Neighbour neighbour = (Neighbour) directNeighbours.get(i);
		    
	    sum += AnisotropicDiffusionPixel.g(neighbour.gradient, bigRegionFunction)*neighbour.gradient;
	}

	double sqrt2div2 = .707106781;
	double sumDiag = 0;

	/*for(int i=0 ; i<diagNeighbours.size() ; i++) {
	    Neighbour neighbour = (Neighbour) diagNeighbours.get(i);
		    
	    sumDiag += AnisotropicDiffusionPixel.g(neighbour.gradient)*neighbour.gradient;
	    }*/

	newHeight = (byte) (currentHeight + lambda*(sum+sqrt2div2*sumDiag));
    }

    /* At the end of the iteration, the new pixel value must
       be switched with the old */
    public void switchHeightValues() {
	currentHeight = newHeight;
    }

    /*After ALL the values have been swithed, the gradient
      must be recalculated */
    public void setGradient() {
	for(int i=0 ; i<directNeighbours.size() ; i++) {
	    Neighbour neighbour = (Neighbour) directNeighbours.get(i);
	
	    neighbour.gradient = neighbour.neighbour.getIntHeight() - getIntHeight();
	}

	/*for(int i=0 ; i<diagNeighbours.size() ; i++) {
	    Neighbour neighbour = (Neighbour) diagNeighbours.get(i);
	
	    neighbour.gradient = neighbour.neighbour.getIntHeight() - getIntHeight();
	    }*/
    }

    /** Function preserving (and enhancing) edges */
    public final static double g(int gradient, boolean bigRegionFunction) {
	if(bigRegionFunction)
	    return 1/(1+Math.pow(gradient/AnisotropicDiffusionPixel.K,2));
	else
	    return Math.exp( - Math.pow( ((double)gradient)/AnisotropicDiffusionPixel.K, 2) );
    }

    public String toString() {
	return new String("("+x+","+y+"), height : "+getIntHeight() ); 
    }

    public final byte getHeight() {
	return currentHeight;
    } 

    public final int getIntHeight() {
	return (int) currentHeight&0xff;
    } 

    public final int getX() {
	return x;
    } 

    public final int getY() {
	return y;
    }

    /*public Vector getNeighbours() {
	return neighbours;
	}*/
}

class Neighbour {
    public AnisotropicDiffusionPixel neighbour;
    public int gradient; /*Approximation of the gradient.*/
    
    public Neighbour(AnisotropicDiffusionPixel neighbour, int gradient) {
	this.neighbour = neighbour;
	this.gradient = gradient;
    }
}
