/**
 * 
 */
package br.com.meslin.faceDetector.visual;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;

import javax.swing.JFrame;

/**
 * @author meslin
 *
 */
public class JCamera extends JFrame {

	/**
	 * Serial version
	 */
	private static final long serialVersionUID = -5825671267165422113L;
	
	/**
	 * Constructor
	 * @throws HeadlessException
	 */
	public JCamera() throws HeadlessException {
	}

	/**
	 * Constructor
	 * @param arg0
	 */
	public JCamera(GraphicsConfiguration arg0) {
		super(arg0);
	}

	/**
	 * Constructor
	 * @param arg0
	 * @throws HeadlessException
	 */
	public JCamera(String arg0) throws HeadlessException {
		super(arg0);
	}

	/**
	 * Constructor
	 * @param arg0
	 * @param arg1
	 */
	public JCamera(String arg0, GraphicsConfiguration arg1) {
		super(arg0, arg1);
	}
}
