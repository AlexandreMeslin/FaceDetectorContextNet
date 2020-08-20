/**
 * 
 */
package br.com.meslin.faceDetector;

import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.apache.commons.lang3.ArrayUtils;
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

import br.com.meslin.faceDetector.visual.JCamera;
import br.com.meslin.util.Debug;

/**
 * @author meslin
 *
 */
public class UDPFaceDetector {
	/** a thread to read a frame, detect faces, and show the image */
	private DaemonThread myThread;
    /** Contextnet Gateway TCP port number */
    private final static int gatewayPort = 5500;

    // OpenCV objects
    /** a video frame */
	private Mat frame;
	private MatOfByte mem;
	/** Contains a frontal face classifier */
	private CascadeClassifier faceDetector;
	/** face-detection data-base file name pointing to haarcascade */
	private final String CASCADE = "haarcascade_frontalface_alt2.xml";
	/** Matrix of rectangles representing detected faces */
	private MatOfRect faceDetections;

	// Visual objects
	private JPanel jPanel;
	private JCamera jCamera;

	/**
	 * 
	 */
	public UDPFaceDetector() {
		// Load OpenCV components
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		jCamera = new JCamera();
		mem = new MatOfByte();
		
		faceDetections = new MatOfRect();
		// to run under Windows, replace 0 by 1 in the next line
		faceDetector = new CascadeClassifier(FaceDetection.class.getResource(CASCADE).getPath().substring(0).replace("%20", " "));
		initComponents();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		UDPFaceDetector thisMain = new UDPFaceDetector();
		thisMain.doAll();
	}
	
	/**
	 * Does everything that is necessary by calling other methods<br>
	 */
	private void doAll() {
		/* Create and display the form */
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				jCamera.setVisible(true);
			}
		});
		
		/* Create the face detector */
		myThread = new DaemonThread();	// create object of threat class
		Thread thread = new Thread(myThread);
		thread.setDaemon(true);
		myThread.runnable = true;
		thread.start();                 // start thread
		
		/* Run UDP server */
		try {
			DatagramSocket socket = new DatagramSocket(gatewayPort);
			System.out.println("READY!!!");
			new ClientHandler(socket).start();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
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
	
	/**
	 * 
	 * @author meslin
	 *
	 */
	class ClientHandler extends Thread {
		private DatagramSocket clientSocket;
		
		/**
		 * 
		 * @param socket
		 */
		public ClientHandler(DatagramSocket socket) {
			this.clientSocket = socket;
		}
		
		/**
		 * Reads the frames
		 */
		public void run() {
			int cols = 0;
			int rows = 0;
			int elemSize = 0;
			int type = 0;
			/** number of bytes read from the stream */
        	int totalToRead;
        	int offset;
			ByteBuffer buffer = ByteBuffer.allocate(8000*8000 *4); 	// 8k image size using 4-byte pixel
            byte[] temp = new byte[8000*8000 *4];
            /** Maximum Trasmiting Unit (in bytes) */
            DatagramPacket packet;
			
			try {
	            while(true) {
	            	packet = new DatagramPacket(temp, temp.length);
	            	clientSocket.receive(packet);
		            // read cols, rows, elemSize, type, totalToRead and offset
	            	/** represents the position inside the ***temp*** buffer */
	            	int position = 0;
	            	cols        = ByteBuffer.wrap(temp, position, 4).getInt(); position += 4;
	            	rows        = ByteBuffer.wrap(temp, position, 4).getInt(); position += 4;
	            	elemSize    = ByteBuffer.wrap(temp, position, 4).getInt(); position += 4;
	            	type        = ByteBuffer.wrap(temp, position, 4).getInt(); position += 4;
	            	totalToRead = ByteBuffer.wrap(temp, position, 4).getInt(); position += 4;
	            	offset      = ByteBuffer.wrap(temp, position, 4).getInt(); position += 4;
	            	buffer.position(offset);
	            	buffer.put(temp, position, totalToRead);
		            frame = matFromByte(buffer, rows, cols, elemSize, type);
	            }
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Creates a Mat from a ByteBuffer
		 * @param buffer
		 * @param rows
		 * @param cols
		 * @param type
		 * @return
		 */
		private Mat matFromByte(ByteBuffer buffer, int rows, int cols, int elemSize, int type) {
	        byte[] bytes = new byte[rows * cols * elemSize];
	        buffer.position(0);
	        buffer.get(bytes, 0, rows * cols * elemSize);
	        return matFromByte(bytes, rows, cols, type);
			
		}
	    /**
	     * Creates a Mat from a byte[]
	     * From: https://stackoverflow.com/questions/27065062/opencv-mat-object-serialization-in-java
	     * @param byteArray
	     * @param rows
	     * @param cols
	     * @param type
	     * @return a Mat object
	     */
		private Mat matFromByte(byte[] byteArray, int rows, int cols, int type) {
	        Mat mat = new Mat(rows, cols, type);
	        mat.put(0, 0, byteArray);
	        return mat;
		}
	    /**
	     * Creates a Mat from a List<Byte>
	     * From: https://stackoverflow.com/questions/27065062/opencv-mat-object-serialization-in-java
	     * @param dataList
	     * @param rows
	     * @param cols
	     * @param type
	     * @return a Mat object
	     */
	    @SuppressWarnings("unused")
		private Mat matFromByte(List<Byte> dataList, int rows, int cols, int type){
	        Byte[] bytes = new Byte[dataList.size()];
	        bytes = dataList.toArray(bytes);
	        // From: https://stackoverflow.com/questions/12944377/how-to-convert-byte-to-byte-and-the-other-way-around
	        return matFromByte(ArrayUtils.toPrimitive(bytes), rows, cols, type);
	    }
	}
	/**
	 * 
	 * @author meslin
	 * 
	 */
	class DaemonThread implements Runnable {
		protected volatile boolean runnable = false;

		@Override
		public void run() {
			synchronized (this) {
				while (runnable) {
					// grab a frame from camera
					if (frame != null) {
						try {
							// retrieve a previously grabbed frame and decode it
							Graphics g = jPanel.getGraphics();
							faceDetector.detectMultiScale(frame, faceDetections);
//							System.out.println(faceDetections.toArray().length + " faces detected");
							for (Rect rect : faceDetections.toArray()) {
								Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255,0));
							}
							//Highgui.imencode(".bmp", frame, mem);
							Imgcodecs.imencode(".png", frame, mem);
							Image im = ImageIO.read(new ByteArrayInputStream(mem.toArray()));
							BufferedImage buff = (BufferedImage) im;
							
							// set form factor according to frame (frame --> mem --> buff)
							int painelW = jCamera.getWidth();
							int painelH = jCamera.getHeight() - 150;
							
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
}
