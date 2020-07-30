/**
 * From: 	https://www.youtube.com/watch?v=thqgjm0pk8A
 * 		 	https://www.youtube.com/watch?v=CVClHLwv-4I
 * 			https://www.youtube.com/watch?v=WeLET1tZPaE
 */
package br.com.meslin.faceDetector;

/**
 * @author meslin
 *
 */
////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Author: Taha Emara
// WebSite : www.Emaraic.com
// E-mail  : taha@emaraic.com
//
// Realtime face detection using OpenCV with Java
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import br.com.meslin.util.Debug;

/**
 *
 * @author Taha Emara
 */
public class FaceDetection extends JFrame {
	/**
	 * Generated serial version ID 
	 */
	private static final long serialVersionUID = 597921564956802333L;

	///
	private DaemonThread myThread;
	private VideoCapture webSource;
	private Mat frame;
	private MatOfByte mem;
	private CascadeClassifier faceDetector;
	private MatOfRect faceDetections;
	///    

	// Visual objects
	private JButton jStartButton;
	private JButton jStopButton;
	private JPanel jPanel;
	
	/** face-detection data-base file name pointing to haarcascade */
	private final String CASCADE = "haarcascade_frontalface_alt2.xml";

	/**
	 * Creates new form FaceDetection
	 */
	public FaceDetection() {
		myThread = null;
		webSource = null;
		frame = new Mat();
		mem = new MatOfByte();
		faceDetections = new MatOfRect();
		// to run under Windows, replace 0 by 1 in the next line
		faceDetector = new CascadeClassifier(FaceDetection.class.getResource(CASCADE).getPath().substring(0).replace("%20", " "));

		initComponents();
	}

	class DaemonThread implements Runnable {
		protected volatile boolean runnable = false;

		@Override
		public void run() {
			synchronized (this) {
				while (runnable) {
					if (webSource.grab()) {
						try {
							webSource.retrieve(frame);
							Graphics g = jPanel.getGraphics();
							faceDetector.detectMultiScale(frame, faceDetections);
							for (Rect rect : faceDetections.toArray()) {
								Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255,0));
							}
							//Highgui.imencode(".bmp", frame, mem);
							Imgcodecs.imencode(".bmp", frame, mem);
							Image im = ImageIO.read(new ByteArrayInputStream(mem.toArray()));
							BufferedImage buff = (BufferedImage) im;
							
							// set form factor according to frame (frame --> mem --> buff)
							int painelW = getWidth();
							int painelH = getHeight() - 150;
							
							if(painelW * buff.getHeight() < painelH * buff.getWidth()) {
								jPanel.setBounds(0, 150, painelW, buff.getHeight() * painelW / buff.getWidth());
							}
							else {
								jPanel.setBounds(0, 150, buff.getWidth() * painelH / buff.getHeight(), painelH);
							}
							
							if (g.drawImage(buff, 0, 0, jPanel.getSize().width, jPanel.getSize().height, 0, 0, buff.getWidth(), buff.getHeight(), null)) {
								if (runnable == false) {
									System.out.println("Paused ..... ");
									this.wait();
								}
							}
						} catch (Exception ex) {
							Debug.error("Error!!", ex);
							ex.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	/**
	 * 
	 */
	private void initComponents() {
		jPanel = new JPanel();
		jStopButton = new JButton();

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

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

		jStopButton.setText("Pause");
		jStopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				jButton2ActionPerformed(evt);
			}
		});
		jStartButton = new JButton();
		
				jStartButton.setText("Start");
				jStartButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						jButton1ActionPerformed(evt);
					}
				});

		GroupLayout layout = new GroupLayout(getContentPane());
		layout.setHorizontalGroup(
			layout.createParallelGroup(Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
							.addComponent(jPanel, GroupLayout.DEFAULT_SIZE, 711, Short.MAX_VALUE)
							.addGap(24))
						.addGroup(Alignment.TRAILING, layout.createSequentialGroup()
							.addComponent(jStartButton)
							.addGap(76)
							.addComponent(jStopButton)
							.addGap(274))))
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addGap(22)
					.addGroup(layout.createParallelGroup(Alignment.BASELINE)
						.addComponent(jStopButton)
						.addComponent(jStartButton))
					.addGap(31)
					.addComponent(jPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(43, Short.MAX_VALUE))
		);
		getContentPane().setLayout(layout);

		pack();
	}

	/**
	 * Stop button
	 * @param evt
	 */
	private void jButton2ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
		myThread.runnable = false;	// stop thread
		jStopButton.setEnabled(false); // activate start button 
		jStartButton.setEnabled(true);  // deactivate stop button

		webSource.release();  		// stop capturing from camera
		webSource = null;			// Free camera
		myThread = null;			// Free thread
	}

	/**
	 * Start button
	 * @param evt
	 */
	private void jButton1ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
		webSource = new VideoCapture(); // video capture
		webSource.open(0);				// capture from camera 0
		myThread = new DaemonThread();	// create object of threat class
		Thread t = new Thread(myThread);
		t.setDaemon(true);
		myThread.runnable = true;
		t.start();                 		// start thread
		jStartButton.setEnabled(false);  	// deactivate start button
		jStopButton.setEnabled(true);  	// activate stop button
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		/* Set the Nimbus look and feel */
		/* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
		 * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
		 */
		try {
			for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(FaceDetection.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			Logger.getLogger(FaceDetection.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			Logger.getLogger(FaceDetection.class.getName()).log(Level.SEVERE, null, ex);
		} catch (UnsupportedLookAndFeelException ex) {
			Logger.getLogger(FaceDetection.class.getName()).log(Level.SEVERE, null, ex);
		}
		

		/* Create and display the form */
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				new FaceDetection().setVisible(true);
			}
		});
	}
}