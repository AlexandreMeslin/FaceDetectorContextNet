/**
 * 
 */
package br.com.meslin.faceDetector;

import java.awt.EventQueue;

import javax.swing.GroupLayout;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.GroupLayout.Alignment;

import org.opencv.core.Core;

import br.com.meslin.faceDetector.visual.JCamera;

/**
 * @author meslin
 *
 */
public class TCPFaceDetector {

	// Visual objects
	private JPanel jPanel;
	private JCamera jCamera;

	/**
	 * 
	 */
	public TCPFaceDetector() {
		// Load OpenCV components
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		jCamera = new JCamera();
		initComponents();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TCPFaceDetector thisMain = new TCPFaceDetector();
		
		/* Create and display the form */
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				thisMain.jCamera.setVisible(true);
			}
		});
	}

	/**
	 * Initialize window layout
	 */
	private void initComponents() {
		jPanel = new JPanel();

		jCamera.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		GroupLayout jPanel1Layout = new GroupLayout(jPanel);
		jPanel1Layout.setHorizontalGroup(
			jPanel1Layout.createParallelGroup(Alignment.LEADING)
				.addGap(0, 711, Short.MAX_VALUE)
		);
		jPanel1Layout.setVerticalGroup(
			jPanel1Layout.createParallelGroup(Alignment.LEADING)
				.addGap(0, 376, Short.MAX_VALUE)
		);
		jPanel.setLayout(jPanel1Layout);
	
		GroupLayout layout = new GroupLayout(jCamera.getContentPane());
		layout.setHorizontalGroup(
			layout.createParallelGroup(Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
							.addComponent(jPanel, GroupLayout.DEFAULT_SIZE, 711, Short.MAX_VALUE)
							.addGap(24))))
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addGap(22)
					.addComponent(jPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(43, Short.MAX_VALUE))
		);
		jCamera.getContentPane().setLayout(layout);

		jCamera.pack();
	}
}
